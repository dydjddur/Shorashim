-- ==============================================================================
-- Shorashim — Complete Backend Fix
-- Run this ONCE in Supabase SQL Editor.
-- ==============================================================================

-- 1. Add missing columns to family_invitations (Phase 6 requirements)
ALTER TABLE public.family_invitations
    ADD COLUMN IF NOT EXISTS uses_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_uses   INT;

-- 2. Create invitation_acceptances table
CREATE TABLE IF NOT EXISTS public.invitation_acceptances (
    invitation_id UUID NOT NULL REFERENCES public.family_invitations(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    accepted_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (invitation_id, user_id)
);

ALTER TABLE public.invitation_acceptances ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "family_admins_can_select_acceptances" ON public.invitation_acceptances;
CREATE POLICY "family_admins_can_select_acceptances"
    ON public.invitation_acceptances
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.family_invitations i
            WHERE i.id = invitation_id
              AND public.has_family_role(i.family_id, ARRAY['OWNER','ADMIN'])
        )
    );

-- 3. Ensure profiles table exists
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT '',
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public profiles are viewable by authenticated users" ON public.profiles;
CREATE POLICY "Public profiles are viewable by authenticated users" ON public.profiles
    FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;
CREATE POLICY "Users can insert their own profile" ON public.profiles
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
CREATE POLICY "Users can update their own profile" ON public.profiles
    FOR UPDATE TO authenticated USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- 4. Ensure memorial_events table exists
CREATE TABLE IF NOT EXISTS public.memorial_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES public.families(id) ON DELETE CASCADE,
    person_id UUID REFERENCES public.people(id) ON DELETE SET NULL,
    person_name TEXT NOT NULL,
    event_date DATE NOT NULL,
    hebrew_date TEXT,
    description TEXT,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_memorial_person_name_not_empty CHECK (char_length(trim(person_name)) >= 1)
);

ALTER TABLE public.memorial_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Members can view memorial events" ON public.memorial_events;
CREATE POLICY "Members can view memorial events" ON public.memorial_events
    FOR SELECT TO authenticated USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can insert memorial events" ON public.memorial_events;
CREATE POLICY "Contributors, admins, owners can insert memorial events" ON public.memorial_events
    FOR INSERT TO authenticated WITH CHECK (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can update memorial events" ON public.memorial_events;
CREATE POLICY "Contributors, admins, owners can update memorial events" ON public.memorial_events
    FOR UPDATE TO authenticated USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    ) WITH CHECK (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can delete memorial events" ON public.memorial_events;
CREATE POLICY "Contributors, admins, owners can delete memorial events" ON public.memorial_events
    FOR DELETE TO authenticated USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

-- 5. accept_invitation RPC (supports both group multi-use links and single-use email links)
CREATE OR REPLACE FUNCTION public.accept_invitation(p_token TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_invitation  public.family_invitations%ROWTYPE;
    v_user_id     UUID := auth.uid();
    v_user_email  TEXT;
    v_family_name TEXT;
    v_is_open     BOOLEAN;
    v_role        TEXT;
BEGIN
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    SELECT email INTO v_user_email FROM auth.users WHERE id = v_user_id;

    SELECT * INTO v_invitation
    FROM public.family_invitations
    WHERE token = p_token
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_not_found');
    END IF;

    v_is_open := v_invitation.invitee_email IS NULL;

    IF v_invitation.status IN ('REVOKED', 'DECLINED') THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_' || v_invitation.status);
    END IF;

    IF v_invitation.status = 'ACCEPTED' AND NOT v_is_open THEN
        RETURN jsonb_build_object('ok', false, 'error', 'already_accepted',
                                  'family_id', v_invitation.family_id);
    END IF;

    IF v_invitation.expires_at < NOW() THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_expired');
    END IF;

    IF NOT v_is_open AND LOWER(v_invitation.invitee_email) <> LOWER(COALESCE(v_user_email, '')) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'email_mismatch');
    END IF;

    SELECT name INTO v_family_name FROM public.families WHERE id = v_invitation.family_id;

    -- Already a member: succeed without consuming a use
    IF EXISTS (
        SELECT 1 FROM public.family_members
        WHERE family_id = v_invitation.family_id AND user_id = v_user_id
    ) THEN
        IF NOT v_is_open THEN
            UPDATE public.family_invitations
            SET status = 'ACCEPTED', invitee_user_id = v_user_id
            WHERE id = v_invitation.id;
        END IF;
        RETURN jsonb_build_object('ok', true, 'already_member', true,
                                  'family_id', v_invitation.family_id,
                                  'family_name', v_family_name);
    END IF;

    IF v_invitation.max_uses IS NOT NULL AND v_invitation.uses_count >= v_invitation.max_uses THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_full');
    END IF;

    v_role := v_invitation.role;
    IF v_is_open AND v_role IN ('OWNER', 'ADMIN') THEN
        v_role := 'CONTRIBUTOR';
    END IF;

    INSERT INTO public.family_members (family_id, user_id, role, invited_by)
    VALUES (v_invitation.family_id, v_user_id, v_role, v_invitation.created_by)
    ON CONFLICT (family_id, user_id) DO NOTHING;

    INSERT INTO public.invitation_acceptances (invitation_id, user_id)
    VALUES (v_invitation.id, v_user_id)
    ON CONFLICT DO NOTHING;

    UPDATE public.family_invitations
    SET uses_count      = COALESCE(uses_count, 0) + 1,
        invitee_user_id = CASE WHEN v_is_open THEN invitee_user_id ELSE v_user_id END,
        status          = CASE WHEN v_is_open THEN status ELSE 'ACCEPTED' END
    WHERE id = v_invitation.id;

    RETURN jsonb_build_object('ok', true, 'family_id', v_invitation.family_id,
                              'family_name', v_family_name);
END;
$$;

-- 6. resolve_invitation RPC (safe for anonymous visitors and authenticated users)
CREATE OR REPLACE FUNCTION public.resolve_invitation(p_token TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
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

    IF v_inv.status = 'ACCEPTED' AND v_inv.invitee_email IS NOT NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'already_accepted');
    END IF;

    IF v_inv.max_uses IS NOT NULL AND v_inv.uses_count >= v_inv.max_uses THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_full');
    END IF;

    SELECT name INTO v_fname FROM public.families WHERE id = v_inv.family_id;

    SELECT COALESCE(
        p.display_name,
        p.full_name,
        u.raw_user_meta_data->>'display_name',
        u.raw_user_meta_data->>'full_name',
        split_part(u.email, '@', 1),
        'בן משפחה'
    )
    INTO v_cname
    FROM auth.users u
    LEFT JOIN public.profiles p ON p.id = u.id
    WHERE u.id = v_inv.created_by;

    IF v_cname IS NULL THEN
        v_cname := 'בן משפחה';
    END IF;

    RETURN jsonb_build_object(
        'ok',            true,
        'invitation_id', v_inv.id,
        'family_id',     v_inv.family_id,
        'family_name',   COALESCE(v_fname, 'עץ משפחה'),
        'invited_by',    v_cname,
        'role',          CASE WHEN v_inv.invitee_email IS NULL AND v_inv.role IN ('OWNER','ADMIN')
                              THEN 'CONTRIBUTOR' ELSE v_inv.role END,
        'status',        v_inv.status,
        'is_open',       v_inv.invitee_email IS NULL,
        'expires_at',    v_inv.expires_at
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.accept_invitation(TEXT)  TO authenticated;
GRANT EXECUTE ON FUNCTION public.resolve_invitation(TEXT) TO anon, authenticated;

