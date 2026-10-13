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

    -- The other person already asked me: accept it instead of creating a crossing request.
    SELECT * INTO v_rev
    FROM public.friend_requests
    WHERE from_user_id = v_target AND to_user_id = v_me AND status = 'PENDING';
    IF FOUND THEN
        UPDATE public.friend_requests SET status = 'ACCEPTED', updated_at = now() WHERE id = v_rev.id;
        INSERT INTO public.friends (user_id, friend_id) VALUES (v_me, v_target) ON CONFLICT DO NOTHING;
        INSERT INTO public.friends (user_id, friend_id) VALUES (v_target, v_me) ON CONFLICT DO NOTHING;
        RETURN jsonb_build_object('ok', true, 'auto_accepted', true, 'user_id', v_target);
    END IF;

    INSERT INTO public.friend_requests (from_user_id, to_user_id)
    VALUES (v_me, v_target)
    ON CONFLICT (from_user_id, to_user_id)
    DO UPDATE SET status = 'PENDING', updated_at = now()
    WHERE public.friend_requests.status <> 'PENDING'
    RETURNING id INTO v_req_id;

    IF v_req_id IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'already_pending');
    END IF;

    RETURN jsonb_build_object('ok', true, 'request_id', v_req_id, 'user_id', v_target);
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

-- 7. Refresh PostgREST's schema cache so the new tables/functions are visible immediately.
NOTIFY pgrst, 'reload schema';
