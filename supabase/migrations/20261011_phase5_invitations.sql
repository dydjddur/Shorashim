-- Phase 5: Family Tree Invitations
-- Run this in your Supabase SQL Editor

-- ============================================================
-- 1. family_invitations table
-- ============================================================
CREATE TABLE IF NOT EXISTS public.family_invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL REFERENCES public.families(id) ON DELETE CASCADE,
    token           TEXT UNIQUE NOT NULL DEFAULT encode(gen_random_bytes(32), 'hex'),
    created_by      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    invitee_email   TEXT,                          -- optional: targeted invite by email
    invitee_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,  -- resolved on claim
    status          TEXT NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','ACCEPTED','DECLINED','REVOKED')),
    role            TEXT NOT NULL DEFAULT 'CONTRIBUTOR'
                        CHECK (role IN ('OWNER','ADMIN','CONTRIBUTOR','EDITOR','VIEWER')),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for fast token look-up
CREATE INDEX IF NOT EXISTS idx_family_invitations_token ON public.family_invitations(token);
-- Index for user's incoming invitations
CREATE INDEX IF NOT EXISTS idx_family_invitations_invitee_email ON public.family_invitations(invitee_email);
CREATE INDEX IF NOT EXISTS idx_family_invitations_invitee_user ON public.family_invitations(invitee_user_id);

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION public.update_invitation_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_invitation_updated_at ON public.family_invitations;
CREATE TRIGGER trg_invitation_updated_at
    BEFORE UPDATE ON public.family_invitations
    FOR EACH ROW EXECUTE FUNCTION public.update_invitation_updated_at();

-- ============================================================
-- 2. Row Level Security
-- ============================================================
ALTER TABLE public.family_invitations ENABLE ROW LEVEL SECURITY;

-- OWNER / ADMIN of the family can SELECT all invitations for their family
CREATE POLICY "family_admins_can_select_invitations"
    ON public.family_invitations
    FOR SELECT
    USING (
        public.has_family_role(family_id, ARRAY['OWNER','ADMIN'])
        OR invitee_user_id = auth.uid()
        OR invitee_email = (SELECT email FROM auth.users WHERE id = auth.uid())
    );

-- OWNER / ADMIN can INSERT invitations for their family
CREATE POLICY "family_admins_can_insert_invitations"
    ON public.family_invitations
    FOR INSERT
    WITH CHECK (
        public.has_family_role(family_id, ARRAY['OWNER','ADMIN'])
        AND created_by = auth.uid()
    );

-- OWNER / ADMIN can UPDATE (revoke) invitations for their family
CREATE POLICY "family_admins_can_update_invitations"
    ON public.family_invitations
    FOR UPDATE
    USING (public.has_family_role(family_id, ARRAY['OWNER','ADMIN']))
    WITH CHECK (public.has_family_role(family_id, ARRAY['OWNER','ADMIN']));

-- Invitee can UPDATE their own invitation (to accept/decline)
CREATE POLICY "invitee_can_update_own_invitation"
    ON public.family_invitations
    FOR UPDATE
    USING (
        invitee_user_id = auth.uid()
        OR invitee_email = (SELECT email FROM auth.users WHERE id = auth.uid())
    )
    WITH CHECK (
        invitee_user_id = auth.uid()
        OR invitee_email = (SELECT email FROM auth.users WHERE id = auth.uid())
    );

-- ============================================================
-- 3. accept_invitation RPC – atomic, backend-validated
-- ============================================================
CREATE OR REPLACE FUNCTION public.accept_invitation(p_token TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_invitation    public.family_invitations%ROWTYPE;
    v_user_id       UUID := auth.uid();
    v_user_email    TEXT;
    v_family_name   TEXT;
BEGIN
    -- Resolve calling user's email
    SELECT email INTO v_user_email FROM auth.users WHERE id = v_user_id;

    -- Lock and fetch the invitation row
    SELECT * INTO v_invitation
    FROM public.family_invitations
    WHERE token = p_token
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_not_found');
    END IF;

    IF v_invitation.status = 'ACCEPTED' THEN
        RETURN jsonb_build_object('ok', false, 'error', 'already_accepted',
                                  'family_id', v_invitation.family_id);
    END IF;

    IF v_invitation.status IN ('REVOKED', 'DECLINED') THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_' || v_invitation.status);
    END IF;

    IF v_invitation.expires_at < NOW() THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_expired');
    END IF;

    -- If invitation targeted a specific email, verify it matches
    IF v_invitation.invitee_email IS NOT NULL
       AND LOWER(v_invitation.invitee_email) <> LOWER(v_user_email) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'email_mismatch');
    END IF;

    -- Check caller is not already a member
    IF EXISTS (
        SELECT 1 FROM public.family_members
        WHERE family_id = v_invitation.family_id AND user_id = v_user_id
    ) THEN
        -- Still mark invitation accepted so UI shows success
        UPDATE public.family_invitations
        SET status = 'ACCEPTED', invitee_user_id = v_user_id
        WHERE id = v_invitation.id;
        SELECT name INTO v_family_name FROM public.families WHERE id = v_invitation.family_id;
        RETURN jsonb_build_object('ok', true, 'already_member', true,
                                  'family_id', v_invitation.family_id,
                                  'family_name', v_family_name);
    END IF;

    -- Insert into family_members
    INSERT INTO public.family_members (family_id, user_id, role, invited_by)
    VALUES (v_invitation.family_id, v_user_id, v_invitation.role, v_invitation.created_by)
    ON CONFLICT (family_id, user_id) DO NOTHING;

    -- Mark invitation accepted
    UPDATE public.family_invitations
    SET status = 'ACCEPTED', invitee_user_id = v_user_id
    WHERE id = v_invitation.id;

    SELECT name INTO v_family_name FROM public.families WHERE id = v_invitation.family_id;

    RETURN jsonb_build_object('ok', true, 'family_id', v_invitation.family_id,
                              'family_name', v_family_name);
END;
$$;

-- ============================================================
-- 4. resolve_invitation RPC – read invitation metadata by token
--    (called before the user accepts, to show the "join" dialog)
-- ============================================================
CREATE OR REPLACE FUNCTION public.resolve_invitation(p_token TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_inv   public.family_invitations%ROWTYPE;
    v_fname TEXT;
    v_cname TEXT;
BEGIN
    SELECT * INTO v_inv FROM public.family_invitations WHERE token = p_token;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_not_found');
    END IF;

    IF v_inv.status = 'REVOKED' THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_revoked');
    END IF;

    IF v_inv.expires_at < NOW() THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_expired');
    END IF;

    SELECT name INTO v_fname FROM public.families WHERE id = v_inv.family_id;
    SELECT COALESCE(p.display_name, p.full_name, u.email)
    INTO v_cname
    FROM auth.users u
    LEFT JOIN public.profiles p ON p.id = u.id
    WHERE u.id = v_inv.created_by;

    RETURN jsonb_build_object(
        'ok',           true,
        'invitation_id', v_inv.id,
        'family_id',    v_inv.family_id,
        'family_name',  v_fname,
        'invited_by',   v_cname,
        'role',         v_inv.role,
        'status',       v_inv.status,
        'expires_at',   v_inv.expires_at
    );
END;
$$;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.accept_invitation(TEXT)  TO authenticated;
GRANT EXECUTE ON FUNCTION public.resolve_invitation(TEXT) TO authenticated;
