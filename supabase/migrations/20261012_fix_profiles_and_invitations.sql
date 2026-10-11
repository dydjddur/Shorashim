-- ==============================================================================
-- Shorashim — Fix missing profiles and memorial_events tables & repair resolve_invitation
-- Run this script in your Supabase SQL Editor.
-- ==============================================================================

-- 1. Ensure public.profiles table exists
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT '',
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_profiles_updated_at ON public.profiles(updated_at);

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

-- 2. Ensure public.memorial_events table exists (preventing "Could not find table memorial_events")
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

CREATE INDEX IF NOT EXISTS idx_memorial_events_family_id ON public.memorial_events(family_id);
CREATE INDEX IF NOT EXISTS idx_memorial_events_event_date ON public.memorial_events(event_date);
CREATE INDEX IF NOT EXISTS idx_memorial_events_person_id ON public.memorial_events(person_id);

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

-- 3. Replace resolve_invitation so it never fails and safely gets inviter display name
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

    -- Look up creator name safely from profiles or auth.users metadata
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

GRANT EXECUTE ON FUNCTION public.resolve_invitation(TEXT) TO anon, authenticated;
