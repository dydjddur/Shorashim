-- ==============================================================================
-- Shorashim (שורשים) — 20261015_real_username_auth.sql
-- Real Username Authentication in Supabase GoTrue, Username Uniqueness,
-- Automatic Profile Provisioning, and Resolution Functions.
-- ==============================================================================

-- 1. Ensure profiles table has username, email, full_name, display_name, and constraints
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS username TEXT,
    ADD COLUMN IF NOT EXISTS email TEXT,
    ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_private BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS bio TEXT DEFAULT '';

-- Unique case-insensitive index on username
CREATE UNIQUE INDEX IF NOT EXISTS uq_profiles_username_lower
    ON public.profiles(LOWER(username))
    WHERE username IS NOT NULL AND username <> '';

-- Format constraint for username: 3 to 30 characters, letters, numbers, underscores
ALTER TABLE public.profiles
    DROP CONSTRAINT IF EXISTS chk_username_format;

ALTER TABLE public.profiles
    ADD CONSTRAINT chk_username_format
    CHECK (username IS NULL OR username ~ '^[a-zA-Z0-9_]{3,30}$');

-- Ensure RLS on profiles allows authenticated users to read all public profiles
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

-- 2. RPC: Check if a username is available (callable by anon before registration)
CREATE OR REPLACE FUNCTION public.check_username_available(p_username TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_clean TEXT := lower(trim(leading '@' from trim(COALESCE(p_username, ''))));
BEGIN
    IF v_clean = '' OR v_clean !~ '^[a-z0-9_]{3,30}$' THEN
        RETURN FALSE;
    END IF;

    -- Check in public.profiles
    IF EXISTS (SELECT 1 FROM public.profiles WHERE lower(username) = v_clean) THEN
        RETURN FALSE;
    END IF;

    -- Check in auth.users (synthetic email or metadata)
    IF EXISTS (
        SELECT 1 FROM auth.users 
        WHERE lower(email) = v_clean || '@shorashim.app'
           OR lower(COALESCE(raw_user_meta_data->>'username', '')) = v_clean
    ) THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$;

GRANT EXECUTE ON FUNCTION public.check_username_available(TEXT) TO anon, authenticated;

-- 3. RPC: Resolve Login Identifier to Supabase GoTrue Email
CREATE OR REPLACE FUNCTION public.resolve_login_email(p_identifier TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_clean TEXT := lower(trim(leading '@' from trim(COALESCE(p_identifier, ''))));
    v_email TEXT;
BEGIN
    IF v_clean = '' THEN
        RETURN NULL;
    END IF;

    -- A. If identifier has '@', check if it directly matches an auth.users email
    IF v_clean ~ '@' THEN
        SELECT email INTO v_email
        FROM auth.users
        WHERE lower(email) = v_clean
        LIMIT 1;

        IF v_email IS NOT NULL THEN
            RETURN v_email;
        END IF;

        -- Check if it matches an email in public.profiles
        SELECT u.email INTO v_email
        FROM public.profiles p
        JOIN auth.users u ON u.id = p.id
        WHERE lower(p.email) = v_clean
        LIMIT 1;

        IF v_email IS NOT NULL THEN
            RETURN v_email;
        END IF;
    END IF;

    -- B. Match in public.profiles by username
    SELECT u.email INTO v_email
    FROM public.profiles p
    JOIN auth.users u ON u.id = p.id
    WHERE lower(p.username) = v_clean
    LIMIT 1;

    IF v_email IS NOT NULL THEN
        RETURN v_email;
    END IF;

    -- C. Match in auth.users by raw_user_meta_data->>'username'
    SELECT email INTO v_email
    FROM auth.users
    WHERE lower(COALESCE(raw_user_meta_data->>'username', '')) = v_clean
    LIMIT 1;

    IF v_email IS NOT NULL THEN
        RETURN v_email;
    END IF;

    -- D. If identifier is a username (no '@'), default to synthetic username email
    IF v_clean !~ '@' THEN
        RETURN v_clean || '@shorashim.app';
    END IF;

    -- E. Default fallback
    RETURN v_clean;
END;
$$;

GRANT EXECUTE ON FUNCTION public.resolve_login_email(TEXT) TO anon, authenticated;

-- 4. Triggers on auth.users for seamless username registration
-- 4A. Before Insert: Auto-confirm synthetic @shorashim.app emails
CREATE OR REPLACE FUNCTION public.handle_new_user_before()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF NEW.email LIKE '%@shorashim.app' THEN
        NEW.email_confirmed_at := COALESCE(NEW.email_confirmed_at, now());
        NEW.confirmed_at := COALESCE(NEW.confirmed_at, now());
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created_before ON auth.users;
CREATE TRIGGER on_auth_user_created_before
    BEFORE INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user_before();

-- 4B. After Insert: Auto-populate public.profiles with username and metadata
CREATE OR REPLACE FUNCTION public.handle_new_user_after()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_username TEXT;
    v_full_name TEXT;
    v_display_name TEXT;
    v_recovery_email TEXT;
BEGIN
    v_username := lower(trim(leading '@' from trim(COALESCE(NEW.raw_user_meta_data->>'username', ''))));
    IF v_username = '' THEN
        IF NEW.email NOT LIKE '%@shorashim.app' THEN
            v_username := split_part(NEW.email, '@', 1);
        ELSE
            v_username := 'user_' || substr(NEW.id::text, 1, 8);
        END IF;
    END IF;

    v_full_name := COALESCE(NULLIF(NEW.raw_user_meta_data->>'full_name', ''), NULLIF(NEW.raw_user_meta_data->>'display_name', ''), v_username);
    v_display_name := COALESCE(NULLIF(NEW.raw_user_meta_data->>'display_name', ''), v_full_name);
    v_recovery_email := NEW.raw_user_meta_data->>'recovery_email';

    INSERT INTO public.profiles (
        id,
        username,
        full_name,
        display_name,
        email,
        is_email_verified
    ) VALUES (
        NEW.id,
        v_username,
        v_full_name,
        v_display_name,
        COALESCE(v_recovery_email, CASE WHEN NEW.email NOT LIKE '%@shorashim.app' THEN NEW.email ELSE NULL END),
        (NEW.email_confirmed_at IS NOT NULL)
    )
    ON CONFLICT (id) DO UPDATE SET
        username = COALESCE(NULLIF(public.profiles.username, ''), EXCLUDED.username),
        full_name = COALESCE(NULLIF(public.profiles.full_name, ''), EXCLUDED.full_name),
        display_name = COALESCE(NULLIF(public.profiles.display_name, ''), EXCLUDED.display_name),
        email = COALESCE(public.profiles.email, EXCLUDED.email),
        updated_at = now();

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created_after ON auth.users;
CREATE TRIGGER on_auth_user_created_after
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user_after();

-- 5. RPC: Set or claim username for an existing authenticated user
CREATE OR REPLACE FUNCTION public.set_my_username(p_username TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_clean TEXT := lower(trim(leading '@' from trim(COALESCE(p_username, ''))));
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

    -- Update profile
    UPDATE public.profiles
    SET username = v_clean,
        updated_at = NOW()
    WHERE id = v_user_id;

    -- Update auth.users metadata so GoTrue knows the username
    UPDATE auth.users
    SET raw_user_meta_data = COALESCE(raw_user_meta_data, '{}'::jsonb) || jsonb_build_object('username', v_clean)
    WHERE id = v_user_id;

    RETURN jsonb_build_object('ok', true, 'username', v_clean);
END;
$$;

GRANT EXECUTE ON FUNCTION public.set_my_username(TEXT) TO authenticated;

-- 6. RPC: Add friend / Send friend request by username or UUID (Immediate bidirectional connection)
CREATE OR REPLACE FUNCTION public.send_friend_request(p_target TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_me     UUID := auth.uid();
    v_target UUID;
    v_clean  TEXT;
    v_req_id UUID;
BEGIN
    IF v_me IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    v_clean := LOWER(TRIM(LEADING '@' FROM TRIM(COALESCE(p_target, ''))));
    IF v_clean = '' THEN
        RETURN jsonb_build_object('ok', false, 'error', 'user_not_found');
    END IF;

    IF v_clean ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN
        SELECT id INTO v_target FROM public.profiles WHERE id = v_clean::UUID;
    ELSE
        SELECT id INTO v_target FROM public.profiles WHERE LOWER(username) = v_clean;
    END IF;

    IF v_target IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'user_not_found');
    END IF;
    IF v_target = v_me THEN
        RETURN jsonb_build_object('ok', false, 'error', 'cannot_add_self');
    END IF;
    IF EXISTS (SELECT 1 FROM public.friends WHERE user_id = v_me AND friend_id = v_target) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'already_friends');
    END IF;

    -- Establish bidirectional friendship immediately and record request as ACCEPTED
    INSERT INTO public.friend_requests (from_user_id, to_user_id, status)
    VALUES (v_me, v_target, 'ACCEPTED')
    ON CONFLICT (from_user_id, to_user_id)
    DO UPDATE SET status = 'ACCEPTED', updated_at = now()
    RETURNING id INTO v_req_id;

    -- Add reciprocal friendship rows so both are friends immediately
    INSERT INTO public.friends (user_id, friend_id) VALUES (v_me, v_target) ON CONFLICT DO NOTHING;
    INSERT INTO public.friends (user_id, friend_id) VALUES (v_target, v_me) ON CONFLICT DO NOTHING;

    RETURN jsonb_build_object('ok', true, 'connected', true, 'user_id', v_target);
END;
$$;

GRANT EXECUTE ON FUNCTION public.send_friend_request(TEXT) TO authenticated;
