-- ==============================================================================
-- Shorashim — APPLY_PENDING.sql
-- Paste this whole file into Supabase Dashboard -> SQL Editor -> Run (safe to run more than once).
--
-- It brings a database that only has migrations up to 20261012 up to date:
--   * profiles.is_private / profiles.bio, families.status 'PRIVATE'
--   * people.linked_user_id + get_mutual_family_connections()
--   * friends, friend_requests (+ send/respond/remove RPCs)
--   * user_public_keys, conversations, chat_messages (E2EE chat) + realtime publication
-- Without it chat, friends and "link person to account" cannot work: those tables/columns do not exist yet.
-- ==============================================================================

-- ############ 20261013_friends_and_privacy.sql ############
-- ==============================================================================
-- Shorashim (שורשים) — Phase 8: Friends, Privacy & Top Panel Support
-- ==============================================================================

-- 1. Add privacy and bio columns to profiles table
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS is_private BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS bio TEXT DEFAULT '';

-- 2. Update families status constraint to support PRIVATE explicitly
ALTER TABLE public.families 
DROP CONSTRAINT IF EXISTS families_status_check;

ALTER TABLE public.families 
ADD CONSTRAINT families_status_check 
CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'PRIVATE'));

-- 3. Friends table
CREATE TABLE IF NOT EXISTS public.friends (
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, friend_id)
);

ALTER TABLE public.friends ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their friends" ON public.friends;
CREATE POLICY "Users can view their friends" ON public.friends
    FOR SELECT TO authenticated USING (
        auth.uid() = user_id OR auth.uid() = friend_id
    );

DROP POLICY IF EXISTS "Users can insert friends" ON public.friends;
CREATE POLICY "Users can insert friends" ON public.friends
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete friends" ON public.friends;
CREATE POLICY "Users can delete friends" ON public.friends
    FOR DELETE TO authenticated USING (auth.uid() = user_id OR auth.uid() = friend_id);

-- 4. Friend Requests table
CREATE TABLE IF NOT EXISTS public.friend_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (from_user_id, to_user_id)
);

ALTER TABLE public.friend_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their requests" ON public.friend_requests;
CREATE POLICY "Users can view their requests" ON public.friend_requests
    FOR SELECT TO authenticated USING (
        auth.uid() = from_user_id OR auth.uid() = to_user_id
    );

DROP POLICY IF EXISTS "Users can create requests" ON public.friend_requests;
CREATE POLICY "Users can create requests" ON public.friend_requests
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = from_user_id);

DROP POLICY IF EXISTS "Users can update their received requests" ON public.friend_requests;
CREATE POLICY "Users can update their received requests" ON public.friend_requests
    FOR UPDATE TO authenticated USING (auth.uid() = to_user_id);

-- ############ 20261013_chat_e2ee_and_person_linking.sql ############
-- ==============================================================================
-- Shorashim (שורשים) — Phase 9: E2EE Chat, Tree Privacy per Tree, and Person-User Linking
-- ==============================================================================

-- 1. Explicitly ensure families status check constraint allows 'PRIVATE'
ALTER TABLE public.families 
DROP CONSTRAINT IF EXISTS families_status_check;

ALTER TABLE public.families 
ADD CONSTRAINT families_status_check 
CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'PRIVATE'));

-- 2. Link person in tree to app user account
ALTER TABLE public.people 
ADD COLUMN IF NOT EXISTS linked_user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_people_linked_user_id ON public.people(linked_user_id);

-- 3. Mutual family connections RPC
CREATE OR REPLACE FUNCTION public.get_mutual_family_connections(p_target_user_id UUID)
RETURNS TABLE (
    family_id UUID,
    family_name TEXT,
    person_id UUID,
    person_name TEXT,
    role TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN;
    END IF;

    RETURN QUERY
    SELECT
        f.id AS family_id,
        f.name AS family_name,
        p.id AS person_id,
        (p.first_name || ' ' || COALESCE(p.last_name, '')) AS person_name,
        COALESCE(fm.role::TEXT, 'MEMBER') AS role
    FROM public.people p
    JOIN public.families f ON f.id = p.family_id
    LEFT JOIN public.family_members fm ON fm.family_id = f.id AND fm.user_id = v_user_id
    WHERE p.linked_user_id = p_target_user_id
      AND (
          f.status = 'PUBLISHED'
          OR f.owner_id = v_user_id
          OR fm.user_id IS NOT NULL
      );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_mutual_family_connections(UUID) TO authenticated;

-- 4. End-to-End Encryption: User Public Keys
CREATE TABLE IF NOT EXISTS public.user_public_keys (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    public_key TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.user_public_keys ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public keys are readable by authenticated users" ON public.user_public_keys;
CREATE POLICY "Public keys are readable by authenticated users" ON public.user_public_keys
    FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "Users can manage their own public key" ON public.user_public_keys;
CREATE POLICY "Users can manage their own public key" ON public.user_public_keys
    FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- 5. Conversations
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant1_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    participant2_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    last_message_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_conversation_participants UNIQUE(participant1_id, participant2_id),
    CONSTRAINT conversation_participants_order CHECK (participant1_id < participant2_id)
);

ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their conversations" ON public.conversations;
CREATE POLICY "Users can view their conversations" ON public.conversations
    FOR SELECT TO authenticated USING (
        auth.uid() = participant1_id OR auth.uid() = participant2_id
    );

DROP POLICY IF EXISTS "Users can insert conversations" ON public.conversations;
CREATE POLICY "Users can insert conversations" ON public.conversations
    FOR INSERT TO authenticated WITH CHECK (
        auth.uid() = participant1_id OR auth.uid() = participant2_id
    );

DROP POLICY IF EXISTS "Users can update conversations" ON public.conversations;
CREATE POLICY "Users can update conversations" ON public.conversations
    FOR UPDATE TO authenticated USING (
        auth.uid() = participant1_id OR auth.uid() = participant2_id
    );

-- 6. Encrypted Chat Messages
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    message_type TEXT NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'INVITATION')),
    ciphertext TEXT NOT NULL,
    iv TEXT NOT NULL,
    encrypted_key_sender TEXT NOT NULL,
    encrypted_key_recipient TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conv ON public.chat_messages(conversation_id, created_at DESC);

ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Participants can view messages" ON public.chat_messages;
CREATE POLICY "Participants can view messages" ON public.chat_messages
    FOR SELECT TO authenticated USING (
        auth.uid() = sender_id OR auth.uid() = recipient_id
    );

DROP POLICY IF EXISTS "Senders can insert messages" ON public.chat_messages;
CREATE POLICY "Senders can insert messages" ON public.chat_messages
    FOR INSERT TO authenticated WITH CHECK (
        auth.uid() = sender_id
    );

DROP POLICY IF EXISTS "Recipients can update message read status" ON public.chat_messages;
CREATE POLICY "Recipients can update message read status" ON public.chat_messages
    FOR UPDATE TO authenticated USING (
        auth.uid() = recipient_id
    );

-- ############ 20261014_friends_chat_realtime.sql ############
-- ==============================================================================
-- Shorashim (שורשים) — Phase 10: server-backed friends + chat realtime
--
-- Run AFTER 20261013_friends_and_privacy.sql and 20261013_chat_e2ee_and_person_linking.sql
-- (supabase/APPLY_PENDING.sql runs all three in the right order).
--
-- Why: until now friend requests were stored only in the sender's SharedPreferences, so the other person never
-- received them. These SECURITY DEFINER functions make friendships real and symmetric, and the realtime publication
-- lets an open chat screen receive new messages instantly.
-- ==============================================================================

-- 1. Send a friend request by @username (or user id). Mutual requests become friendships automatically.
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
    v_rev    public.friend_requests%ROWTYPE;
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

-- 2. Accept / decline an incoming request (accepting creates BOTH friendship rows).
CREATE OR REPLACE FUNCTION public.respond_friend_request(p_request_id UUID, p_accept BOOLEAN)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_me  UUID := auth.uid();
    v_req public.friend_requests%ROWTYPE;
BEGIN
    IF v_me IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    SELECT * INTO v_req
    FROM public.friend_requests
    WHERE id = p_request_id AND to_user_id = v_me AND status = 'PENDING'
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'request_not_found');
    END IF;

    UPDATE public.friend_requests
    SET status = CASE WHEN p_accept THEN 'ACCEPTED' ELSE 'DECLINED' END, updated_at = now()
    WHERE id = v_req.id;

    IF p_accept THEN
        INSERT INTO public.friends (user_id, friend_id) VALUES (v_me, v_req.from_user_id) ON CONFLICT DO NOTHING;
        INSERT INTO public.friends (user_id, friend_id) VALUES (v_req.from_user_id, v_me) ON CONFLICT DO NOTHING;
    END IF;

    RETURN jsonb_build_object('ok', true, 'accepted', p_accept);
END;
$$;

-- 3. Incoming pending requests together with the sender's public profile data.
CREATE OR REPLACE FUNCTION public.get_my_friend_requests()
RETURNS TABLE (
    id UUID,
    from_user_id UUID,
    username TEXT,
    display_name TEXT,
    full_name TEXT,
    avatar_url TEXT,
    created_at TIMESTAMPTZ
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
    SELECT r.id, r.from_user_id, p.username, p.display_name, p.full_name, p.avatar_url, r.created_at
    FROM public.friend_requests r
    JOIN public.profiles p ON p.id = r.from_user_id
    WHERE r.to_user_id = auth.uid() AND r.status = 'PENDING'
    ORDER BY r.created_at DESC;
$$;

-- 4. Remove a friendship in both directions.
CREATE OR REPLACE FUNCTION public.remove_friend(p_friend_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_me UUID := auth.uid();
BEGIN
    IF v_me IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;
    DELETE FROM public.friends
    WHERE (user_id = v_me AND friend_id = p_friend_id) OR (user_id = p_friend_id AND friend_id = v_me);
    RETURN jsonb_build_object('ok', true);
END;
$$;

GRANT EXECUTE ON FUNCTION public.send_friend_request(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.respond_friend_request(UUID, BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_my_friend_requests() TO authenticated;
GRANT EXECUTE ON FUNCTION public.remove_friend(UUID) TO authenticated;

-- 5. Chat: make sure a message can only be inserted into a conversation the sender belongs to, and that the
--    recipient is the other participant (prevents injecting messages into someone else's thread).
DROP POLICY IF EXISTS "Senders can insert messages" ON public.chat_messages;
CREATE POLICY "Senders can insert messages" ON public.chat_messages
    FOR INSERT TO authenticated WITH CHECK (
        auth.uid() = sender_id
        AND EXISTS (
            SELECT 1 FROM public.conversations c
            WHERE c.id = conversation_id
              AND (
                  (c.participant1_id = sender_id AND c.participant2_id = recipient_id)
                  OR (c.participant2_id = sender_id AND c.participant1_id = recipient_id)
              )
        )
    );

-- 6. Realtime: open chats receive new messages / read receipts instantly.
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['chat_messages', 'conversations', 'friend_requests', 'friends']
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
        END IF;
    END LOOP;
END $$;

-- ############ 20261015_real_username_auth.sql ############
-- ==============================================================================
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

-- 7. Refresh PostgREST's schema cache so the new tables/functions are visible immediately.
NOTIFY pgrst, 'reload schema';



-- ===== 20261020_chat_key_backups.sql =====
-- Chat key backups: lets chat history survive app reinstalls / new devices.
--
-- Each row holds ONE chat private key encrypted on the device with a key derived from the user's password
-- (PBKDF2 + AES-256-GCM). The server never sees the password or the plain private key, so chats stay end-to-end
-- encrypted. Only the owner can read or write their rows.

create table if not exists public.user_key_backups (
    user_id uuid not null references auth.users (id) on delete cascade,
    key_fingerprint text not null,
    public_key text not null,
    wrapped_private_key text not null,
    salt text not null,
    iv text not null,
    kdf text not null default 'PBKDF2WithHmacSHA256',
    iterations integer not null default 150000,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, key_fingerprint)
);

alter table public.user_key_backups enable row level security;

drop policy if exists "key backups: owner select" on public.user_key_backups;
create policy "key backups: owner select" on public.user_key_backups
    for select using (auth.uid() = user_id);

drop policy if exists "key backups: owner insert" on public.user_key_backups;
create policy "key backups: owner insert" on public.user_key_backups
    for insert with check (auth.uid() = user_id);

drop policy if exists "key backups: owner update" on public.user_key_backups;
create policy "key backups: owner update" on public.user_key_backups
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "key backups: owner delete" on public.user_key_backups;
create policy "key backups: owner delete" on public.user_key_backups
    for delete using (auth.uid() = user_id);

grant select, insert, update, delete on public.user_key_backups to authenticated;
