-- ==============================================================================
-- Shorashim — Multi-Feature Upgrade: Username Auth, In-App Invitations,
-- Real-time Subscriptions, and Memorial Automation
-- Run this ONCE in Supabase SQL Editor.
-- ==============================================================================

-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
CREATE EXTENSION IF NOT EXISTS pgjwt WITH SCHEMA extensions;

-- 2. Profiles Table: Add Username, Optional Email, and Unique Index
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS username TEXT,
    ADD COLUMN IF NOT EXISTS email TEXT,
    ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure case-insensitive uniqueness for usernames
CREATE UNIQUE INDEX IF NOT EXISTS uq_profiles_username_lower
    ON public.profiles(LOWER(username))
    WHERE username IS NOT NULL AND username <> '';

-- Username format constraint: alphanumeric and underscore, 3 to 30 characters
ALTER TABLE public.profiles
    DROP CONSTRAINT IF EXISTS chk_username_format;

ALTER TABLE public.profiles
    ADD CONSTRAINT chk_username_format
    CHECK (username IS NULL OR username ~ '^[a-zA-Z0-9_]{3,30}$');

-- Ensure family_invitations columns exist
ALTER TABLE public.family_invitations
    ADD COLUMN IF NOT EXISTS uses_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_uses INT DEFAULT NULL;

-- 3. Secure User Credentials Table (Passwords hashed with Bcrypt)
CREATE TABLE IF NOT EXISTS public.user_credentials (
    user_id       UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RLS: User credentials MUST NEVER be accessible directly via PostgREST
ALTER TABLE public.user_credentials ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "No direct access to credentials" ON public.user_credentials;
CREATE POLICY "No direct access to credentials"
    ON public.user_credentials
    FOR ALL
    TO PUBLIC
    USING (false);

-- 4. Password Reset Codes Table (OTP)
CREATE TABLE IF NOT EXISTS public.password_reset_codes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    code_hash     TEXT NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    attempts_left INT NOT NULL DEFAULT 5,
    consumed      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.password_reset_codes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "No direct access to reset codes" ON public.password_reset_codes;
CREATE POLICY "No direct access to reset codes"
    ON public.password_reset_codes
    FOR ALL
    TO PUBLIC
    USING (false);

-- 5. RPC: Search User By Username (Safe Preview for In-App Invitations)
DROP FUNCTION IF EXISTS public.search_user_by_username(TEXT);
CREATE OR REPLACE FUNCTION public.search_user_by_username(p_username TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_profile public.profiles%ROWTYPE;
BEGIN
    IF p_username IS NULL OR trim(p_username) = '' THEN
        RETURN jsonb_build_object('found', false);
    END IF;

    SELECT * INTO v_profile
    FROM public.profiles
    WHERE LOWER(username) = LOWER(trim(p_username))
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('found', false);
    END IF;

    RETURN jsonb_build_object(
        'found', true,
        'id', v_profile.id,
        'username', v_profile.username,
        'display_name', COALESCE(NULLIF(v_profile.display_name, ''), NULLIF(v_profile.full_name, ''), v_profile.username),
        'avatar_url', v_profile.avatar_url
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.search_user_by_username(TEXT) TO authenticated;

-- 6. RPC: Send In-App Tree Invitation by Username
DROP FUNCTION IF EXISTS public.send_tree_invitation(UUID, TEXT, TEXT);
CREATE OR REPLACE FUNCTION public.send_tree_invitation(
    p_family_id UUID,
    p_invitee_username TEXT,
    p_role TEXT DEFAULT 'CONTRIBUTOR'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_inviter_id UUID := auth.uid();
    v_invitee_profile public.profiles%ROWTYPE;
    v_new_invitation_id UUID;
    v_family_exists BOOLEAN;
    v_token TEXT;
BEGIN
    IF v_inviter_id IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    -- Verify inviter has OWNER or ADMIN role on the family tree
    IF NOT public.has_family_role(p_family_id, ARRAY['OWNER', 'ADMIN']) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'unauthorized');
    END IF;

    -- Verify family tree exists
    SELECT EXISTS (SELECT 1 FROM public.families WHERE id = p_family_id) INTO v_family_exists;
    IF NOT v_family_exists THEN
        RETURN jsonb_build_object('ok', false, 'error', 'family_not_found');
    END IF;

    -- Find invitee profile by lowercase username
    SELECT * INTO v_invitee_profile
    FROM public.profiles
    WHERE LOWER(username) = LOWER(trim(p_invitee_username));

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'user_not_found');
    END IF;

    -- Cannot invite oneself
    IF v_invitee_profile.id = v_inviter_id THEN
        RETURN jsonb_build_object('ok', false, 'error', 'cannot_invite_self');
    END IF;

    -- Check if invitee is already a member of this tree
    IF EXISTS (
        SELECT 1 FROM public.family_members
        WHERE family_id = p_family_id AND user_id = v_invitee_profile.id
    ) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'already_member');
    END IF;

    -- Check if there is already a pending invitation for this user
    IF EXISTS (
        SELECT 1 FROM public.family_invitations
        WHERE family_id = p_family_id 
          AND invitee_user_id = v_invitee_profile.id
          AND status = 'PENDING'
          AND (expires_at IS NULL OR expires_at > NOW())
    ) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_already_pending');
    END IF;

    v_token := extensions.gen_random_uuid()::text;

    INSERT INTO public.family_invitations (
        family_id,
        token,
        created_by,
        invitee_user_id,
        invitee_email,
        status,
        role,
        expires_at
    ) VALUES (
        p_family_id,
        v_token,
        v_inviter_id,
        v_invitee_profile.id,
        v_invitee_profile.email,
        'PENDING',
        COALESCE(p_role, 'CONTRIBUTOR'),
        NOW() + INTERVAL '30 days'
    )
    RETURNING id INTO v_new_invitation_id;

    RETURN jsonb_build_object(
        'ok', true,
        'invitation_id', v_new_invitation_id,
        'invitee_user_id', v_invitee_profile.id,
        'username', v_invitee_profile.username
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.send_tree_invitation(UUID, TEXT, TEXT) TO authenticated;

-- 7. RPC: Get My Pending Invitations Enriched with Inviter and Tree Details
DROP FUNCTION IF EXISTS public.get_my_pending_invitations() CASCADE;
CREATE OR REPLACE FUNCTION public.get_my_pending_invitations()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_result JSONB;
BEGIN
    IF v_user_id IS NULL THEN
        RETURN '[]'::jsonb;
    END IF;

    SELECT COALESCE(jsonb_agg(
        jsonb_build_object(
            'id', i.id,
            'family_id', i.family_id,
            'family_name', f.name,
            'token', i.token,
            'created_by', i.created_by,
            'inviter_display_name', COALESCE(NULLIF(inv_p.display_name, ''), NULLIF(inv_p.full_name, ''), inv_p.username, 'מנהל העץ'),
            'inviter_username', inv_p.username,
            'status', i.status,
            'role', i.role,
            'created_at', i.created_at,
            'expires_at', i.expires_at
        )
    ), '[]'::jsonb)
    INTO v_result
    FROM public.family_invitations i
    JOIN public.families f ON f.id = i.family_id
    LEFT JOIN public.profiles inv_p ON inv_p.id = i.created_by
    WHERE i.invitee_user_id = v_user_id
      AND i.status = 'PENDING'
      AND (i.expires_at IS NULL OR i.expires_at > NOW());

    RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_my_pending_invitations() TO authenticated;

-- 8. RPC: Accept In-App Tree Invitation by ID
DROP FUNCTION IF EXISTS public.accept_tree_invitation(UUID);
CREATE OR REPLACE FUNCTION public.accept_tree_invitation(p_invitation_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_inv public.family_invitations%ROWTYPE;
    v_family_name TEXT;
BEGIN
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    SELECT * INTO v_inv
    FROM public.family_invitations
    WHERE id = p_invitation_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_not_found');
    END IF;

    IF v_inv.invitee_user_id IS NOT NULL AND v_inv.invitee_user_id <> v_user_id THEN
        RETURN jsonb_build_object('ok', false, 'error', 'unauthorized_recipient');
    END IF;

    IF v_inv.status = 'ACCEPTED' THEN
        RETURN jsonb_build_object('ok', false, 'error', 'already_accepted');
    END IF;

    IF v_inv.status IN ('DECLINED', 'REVOKED') THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_' || LOWER(v_inv.status));
    END IF;

    IF v_inv.expires_at IS NOT NULL AND v_inv.expires_at < NOW() THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_expired');
    END IF;

    -- Check if tree still exists
    SELECT name INTO v_family_name FROM public.families WHERE id = v_inv.family_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'tree_deleted');
    END IF;

    -- Add to family_members if not already member
    INSERT INTO public.family_members (family_id, user_id, role, invited_by)
    VALUES (v_inv.family_id, v_user_id, v_inv.role, v_inv.created_by)
    ON CONFLICT (family_id, user_id) DO UPDATE SET role = EXCLUDED.role;

    -- Mark invitation accepted
    UPDATE public.family_invitations
    SET status = 'ACCEPTED',
        uses_count = COALESCE(uses_count, 0) + 1,
        updated_at = NOW()
    WHERE id = p_invitation_id;

    RETURN jsonb_build_object(
        'ok', true,
        'family_id', v_inv.family_id,
        'family_name', v_family_name
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.accept_tree_invitation(UUID) TO authenticated;

-- 9. RPC: Decline In-App Tree Invitation by ID
DROP FUNCTION IF EXISTS public.decline_tree_invitation(UUID);
CREATE OR REPLACE FUNCTION public.decline_tree_invitation(p_invitation_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID := auth.uid();
BEGIN
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    UPDATE public.family_invitations
    SET status = 'DECLINED', updated_at = NOW()
    WHERE id = p_invitation_id AND invitee_user_id = v_user_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invitation_not_found');
    END IF;

    RETURN jsonb_build_object('ok', true);
END;
$$;

GRANT EXECUTE ON FUNCTION public.decline_tree_invitation(UUID) TO authenticated;

-- 10. RPC: Set One-Time Username for Existing User
DROP FUNCTION IF EXISTS public.set_my_username(TEXT);
CREATE OR REPLACE FUNCTION public.set_my_username(p_username TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_clean TEXT := lower(trim(p_username));
    v_exists BOOLEAN;
BEGIN
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    IF v_clean !~ '^[a-z0-9_]{3,30}$' THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invalid_username_format');
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.profiles 
        WHERE LOWER(username) = v_clean AND id <> v_user_id
    ) INTO v_exists;

    IF v_exists THEN
        RETURN jsonb_build_object('ok', false, 'error', 'username_taken');
    END IF;

    UPDATE public.profiles
    SET username = trim(p_username),
        updated_at = NOW()
    WHERE id = v_user_id;

    RETURN jsonb_build_object('ok', true, 'username', trim(p_username));
END;
$$;

GRANT EXECUTE ON FUNCTION public.set_my_username(TEXT) TO authenticated;

-- 11. RPC: Request Password Reset Code
DROP FUNCTION IF EXISTS public.request_password_reset_code(TEXT, TEXT);
CREATE OR REPLACE FUNCTION public.request_password_reset_code(
    p_username TEXT,
    p_code_hash TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_profile public.profiles%ROWTYPE;
    v_last_request TIMESTAMPTZ;
BEGIN
    -- Prevent enumeration attacks: always return identical success message structure
    SELECT * INTO v_profile
    FROM public.profiles
    WHERE LOWER(username) = LOWER(trim(p_username));

    IF NOT FOUND OR v_profile.email IS NULL OR trim(v_profile.email) = '' THEN
        RETURN jsonb_build_object('ok', true, 'has_email', false);
    END IF;

    -- Cooldown: check if a reset code was created within the last 60 seconds
    SELECT created_at INTO v_last_request
    FROM public.password_reset_codes
    WHERE user_id = v_profile.id
    ORDER BY created_at DESC
    LIMIT 1;

    IF v_last_request IS NOT NULL AND v_last_request > (NOW() - INTERVAL '60 seconds') THEN
        RETURN jsonb_build_object('ok', false, 'error', 'cooldown_active');
    END IF;

    -- Invalidate previous unused codes
    UPDATE public.password_reset_codes
    SET consumed = true
    WHERE user_id = v_profile.id AND consumed = false;

    -- Insert new code with 15-minute expiration
    INSERT INTO public.password_reset_codes (
        user_id,
        code_hash,
        expires_at
    ) VALUES (
        v_profile.id,
        p_code_hash,
        NOW() + INTERVAL '15 minutes'
    );

    RETURN jsonb_build_object(
        'ok', true,
        'has_email', true,
        'email_masked', regexp_replace(v_profile.email, '(?<=.).(?=.*@)', '*', 'g')
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.request_password_reset_code(TEXT, TEXT) TO anon, authenticated;

-- 12. RPC: Verify Reset Code and Change Password
DROP FUNCTION IF EXISTS public.verify_and_reset_password(TEXT, TEXT, TEXT);
CREATE OR REPLACE FUNCTION public.verify_and_reset_password(
    p_username TEXT,
    p_code_hash TEXT,
    p_new_password_hash TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_profile public.profiles%ROWTYPE;
    v_code_record public.password_reset_codes%ROWTYPE;
BEGIN
    SELECT * INTO v_profile
    FROM public.profiles
    WHERE LOWER(username) = LOWER(trim(p_username));

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invalid_credentials');
    END IF;

    SELECT * INTO v_code_record
    FROM public.password_reset_codes
    WHERE user_id = v_profile.id AND consumed = false
    ORDER BY created_at DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'code_not_found');
    END IF;

    IF v_code_record.expires_at < NOW() THEN
        RETURN jsonb_build_object('ok', false, 'error', 'code_expired');
    END IF;

    IF v_code_record.attempts_left <= 0 THEN
        RETURN jsonb_build_object('ok', false, 'error', 'max_attempts_exceeded');
    END IF;

    IF v_code_record.code_hash <> p_code_hash THEN
        UPDATE public.password_reset_codes
        SET attempts_left = attempts_left - 1
        WHERE id = v_code_record.id;
        RETURN jsonb_build_object('ok', false, 'error', 'invalid_code', 'attempts_left', v_code_record.attempts_left - 1);
    END IF;

    -- Code is valid! Consume it
    UPDATE public.password_reset_codes
    SET consumed = true
    WHERE id = v_code_record.id;

    -- Update password in user_credentials
    INSERT INTO public.user_credentials (user_id, password_hash, updated_at)
    VALUES (v_profile.id, p_new_password_hash, NOW())
    ON CONFLICT (user_id) DO UPDATE SET password_hash = EXCLUDED.password_hash, updated_at = NOW();

    RETURN jsonb_build_object('ok', true);
END;
$$;

GRANT EXECUTE ON FUNCTION public.verify_and_reset_password(TEXT, TEXT, TEXT) TO anon, authenticated;

-- 13. Enable Supabase Realtime Publication for Essential Tables
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'people'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.people;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'relationships'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.relationships;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'family_members'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.family_members;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'family_invitations'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.family_invitations;
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Realtime publication setup skipped or already exists: %', SQLERRM;
END;
$$;

-- 14. RPC: Register With Username (Native Username Auth)
DROP FUNCTION IF EXISTS public.register_with_username(TEXT, TEXT, TEXT, TEXT);
CREATE OR REPLACE FUNCTION public.register_with_username(
    p_name TEXT,
    p_username TEXT,
    p_password TEXT,
    p_email TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions, pg_temp
AS $$
DECLARE
    v_clean_username TEXT := lower(trim(p_username));
    v_clean_email TEXT := NULLIF(lower(trim(COALESCE(p_email, ''))), '');
    v_user_id UUID;
    v_password_hash TEXT;
    v_jwt_secret TEXT;
    v_token TEXT;
BEGIN
    IF v_clean_username !~ '^[a-z0-9_]{3,30}$' THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invalid_username_format');
    END IF;

    IF EXISTS (SELECT 1 FROM public.profiles WHERE LOWER(username) = v_clean_username) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'username_taken');
    END IF;

    IF char_length(p_password) < 6 THEN
        RETURN jsonb_build_object('ok', false, 'error', 'password_too_short');
    END IF;

    v_user_id := extensions.gen_random_uuid();
    v_password_hash := extensions.crypt(p_password, extensions.gen_salt('bf', 10));

    INSERT INTO public.profiles (
        id,
        username,
        full_name,
        display_name,
        email,
        is_email_verified
    ) VALUES (
        v_user_id,
        trim(p_username),
        trim(p_name),
        trim(p_name),
        v_clean_email,
        (v_clean_email IS NOT NULL)
    );

    INSERT INTO public.user_credentials (
        user_id,
        password_hash
    ) VALUES (
        v_user_id,
        v_password_hash
    );

    BEGIN
        v_jwt_secret := current_setting('app.settings.jwt_secret', true);
    EXCEPTION WHEN OTHERS THEN
        v_jwt_secret := NULL;
    END;

    IF v_jwt_secret IS NOT NULL AND trim(v_jwt_secret) <> '' THEN
        v_token := extensions.sign(
            json_build_object(
                'sub', v_user_id::text,
                'role', 'authenticated',
                'aud', 'authenticated',
                'exp', extract(epoch from now() + interval '30 days')::integer
            ),
            v_jwt_secret,
            'HS256'
        );
    END IF;

    RETURN jsonb_build_object(
        'ok', true,
        'user_id', v_user_id,
        'username', trim(p_username),
        'display_name', trim(p_name),
        'email', v_clean_email,
        'token', v_token
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.register_with_username(TEXT, TEXT, TEXT, TEXT) TO anon, authenticated;

-- 15. RPC: Login With Username (Native Username Auth)
DROP FUNCTION IF EXISTS public.login_with_username(TEXT, TEXT);
CREATE OR REPLACE FUNCTION public.login_with_username(
    p_username_or_email TEXT,
    p_password TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions, pg_temp
AS $$
DECLARE
    v_clean TEXT := lower(trim(p_username_or_email));
    v_profile public.profiles%ROWTYPE;
    v_cred public.user_credentials%ROWTYPE;
    v_jwt_secret TEXT;
    v_token TEXT;
BEGIN
    SELECT * INTO v_profile
    FROM public.profiles
    WHERE LOWER(username) = v_clean OR (email IS NOT NULL AND LOWER(email) = v_clean)
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invalid_credentials');
    END IF;

    SELECT * INTO v_cred
    FROM public.user_credentials
    WHERE user_id = v_profile.id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'legacy_auth_required', 'email', v_profile.email);
    END IF;

    IF extensions.crypt(p_password, v_cred.password_hash) <> v_cred.password_hash THEN
        RETURN jsonb_build_object('ok', false, 'error', 'invalid_credentials');
    END IF;

    BEGIN
        v_jwt_secret := current_setting('app.settings.jwt_secret', true);
    EXCEPTION WHEN OTHERS THEN
        v_jwt_secret := NULL;
    END;

    IF v_jwt_secret IS NOT NULL AND trim(v_jwt_secret) <> '' THEN
        v_token := extensions.sign(
            json_build_object(
                'sub', v_profile.id::text,
                'role', 'authenticated',
                'aud', 'authenticated',
                'exp', extract(epoch from now() + interval '30 days')::integer
            ),
            v_jwt_secret,
            'HS256'
        );
    END IF;

    RETURN jsonb_build_object(
        'ok', true,
        'user_id', v_profile.id,
        'username', v_profile.username,
        'display_name', COALESCE(NULLIF(v_profile.display_name, ''), NULLIF(v_profile.full_name, ''), v_profile.username),
        'email', v_profile.email,
        'token', v_token
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.login_with_username(TEXT, TEXT) TO anon, authenticated;
