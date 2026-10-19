-- ==============================================================================
-- Shorashim (שורשים) — FIX_ALL_USERNAMES_AND_FRIENDS.sql
-- גרסה מתוקנת ומלאה לפתרון בעיות כפילות שמות משתמש (Error 23505) והצגת משתמשים באפליקציה
-- 
-- הוראות הפעלה:
-- 1. העתק את כל הקוד בקובץ זה.
-- 2. פתח את Supabase Dashboard -> פרויקט Shorashim -> SQL Editor.
-- 3. הדבק את הקוד ולחץ על "Run" (הכפתור הירוק).
-- ==============================================================================

-- 1. הרחבת והבטחת עמודות הפרופיל והפרטיות
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS username TEXT;

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS full_name TEXT;

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS display_name TEXT;

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS email TEXT;

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS avatar_url TEXT;

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS bio TEXT DEFAULT '';

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS is_private BOOLEAN NOT NULL DEFAULT false;

-- איפוס פרטיות ל-false לכל מי שמוגדר כ-null כדי שיופיע תמיד באפליקציה
UPDATE public.profiles 
SET is_private = false 
WHERE is_private IS NULL;

-- 2. הסרה זמנית של האינדקס הייחודי כדי למנוע קריסות (Error 23505) בזמן תיקון הכפילויות
DROP INDEX IF EXISTS public.uq_profiles_username_lower;
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS uq_profiles_username_lower;
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS chk_username_format;

-- 3. בלוק PL/pgSQL חכם לתיקון, סנכרון ופתרון כפילויות של שמות משתמש
DO $$
DECLARE
    u_rec RECORD;
    v_candidate TEXT;
    v_final TEXT;
    v_suffix INT;
    v_full_name TEXT;
    v_display_name TEXT;
    v_profile_exists BOOLEAN;
BEGIN
    -- לולאה על כל המשתמשים בסופבייס לפי סדר יצירתם (המשתמש הוותיק יותר שומר על שמו המקורי)
    FOR u_rec IN 
        SELECT 
            u.id,
            u.email,
            u.raw_user_meta_data,
            u.created_at,
            p.username AS current_username,
            p.full_name AS current_full_name,
            p.display_name AS current_display_name
        FROM auth.users u
        LEFT JOIN public.profiles p ON p.id = u.id
        ORDER BY u.created_at ASC NULLS LAST
    LOOP
        -- קביעת שם משתמש מועמד ראשוני
        IF u_rec.current_username IS NOT NULL AND TRIM(u_rec.current_username) <> '' THEN
            v_candidate := u_rec.current_username;
        ELSE
            v_candidate := COALESCE(
                NULLIF(u_rec.raw_user_meta_data->>'username', ''),
                NULLIF(u_rec.raw_user_meta_data->>'user_name', ''),
                CASE 
                    WHEN u_rec.email IS NOT NULL AND u_rec.email NOT LIKE '%@shorashim.app' 
                    THEN split_part(u_rec.email, '@', 1) 
                    ELSE 'user_' || substr(u_rec.id::text, 1, 8) 
                END
            );
        END IF;

        -- ניקוי השם: אותיות קטנות בלבד, הסרת @, והמרת כל תו לא חוקי לקו תחתון
        v_candidate := LOWER(TRIM(LEADING '@' FROM TRIM(COALESCE(v_candidate, ''))));
        v_candidate := regexp_replace(v_candidate, '[^a-z0-9_]', '_', 'g');
        v_candidate := trim(both '_' from v_candidate);

        -- ודא אורך מינימלי של 3 תווים
        IF length(v_candidate) < 3 THEN
            v_candidate := 'user_' || substr(u_rec.id::text, 1, 6);
        END IF;

        -- קיצור אם ארוך מדי (משאירים מקום לסיומת ספרות אם יש כפילות)
        IF length(v_candidate) > 22 THEN
            v_candidate := substr(v_candidate, 1, 22);
        END IF;

        -- בדיקת כפילות: אם השם כבר שייך לחשבון אחר, מוסיפים סיומת _2, _3 וכו'
        v_final := v_candidate;
        v_suffix := 1;
        WHILE EXISTS (
            SELECT 1 
            FROM public.profiles 
            WHERE LOWER(username) = v_final 
              AND id <> u_rec.id
        ) LOOP
            v_suffix := v_suffix + 1;
            v_final := substr(v_candidate, 1, 20) || '_' || v_suffix::text;
        END LOOP;

        -- הכנת שמות תצוגה
        v_full_name := COALESCE(
            NULLIF(u_rec.current_full_name, ''),
            NULLIF(u_rec.raw_user_meta_data->>'full_name', ''), 
            NULLIF(u_rec.raw_user_meta_data->>'display_name', ''), 
            v_final,
            'משתמש'
        );

        v_display_name := COALESCE(
            NULLIF(u_rec.current_display_name, ''),
            NULLIF(u_rec.raw_user_meta_data->>'display_name', ''), 
            NULLIF(u_rec.raw_user_meta_data->>'full_name', ''), 
            v_full_name
        );

        -- עדכון או הוספת הפרופיל בבטחה
        SELECT EXISTS (SELECT 1 FROM public.profiles WHERE id = u_rec.id) INTO v_profile_exists;

        IF v_profile_exists THEN
            UPDATE public.profiles
            SET username = v_final,
                full_name = v_full_name,
                display_name = v_display_name,
                email = COALESCE(public.profiles.email, u_rec.email),
                is_private = false,
                updated_at = now()
            WHERE id = u_rec.id;
        ELSE
            INSERT INTO public.profiles (
                id,
                username,
                full_name,
                display_name,
                email,
                is_private,
                created_at,
                updated_at
            ) VALUES (
                u_rec.id,
                v_final,
                v_full_name,
                v_display_name,
                u_rec.email,
                false,
                COALESCE(u_rec.created_at, now()),
                now()
            );
        END IF;

        -- סנכרון שם המשתמש גם לתוך המטא-דאטה של auth.users
        UPDATE auth.users
        SET raw_user_meta_data = COALESCE(raw_user_meta_data, '{}'::jsonb) || jsonb_build_object('username', v_final)
        WHERE id = u_rec.id;

    END LOOP;
END $$;

-- 4. תיקון כל פרופיל שנותר ללא שם משתמש חוקי
DO $$
DECLARE
    p_rec RECORD;
    v_clean TEXT;
    v_final TEXT;
    v_suffix INT;
BEGIN
    FOR p_rec IN 
        SELECT id, username 
        FROM public.profiles 
        WHERE username IS NULL OR username = '' OR username !~ '^[a-zA-Z0-9_]{3,30}$'
    LOOP
        v_clean := 'user_' || substr(p_rec.id::text, 1, 8);
        v_final := v_clean;
        v_suffix := 1;
        WHILE EXISTS (SELECT 1 FROM public.profiles WHERE LOWER(username) = v_final AND id <> p_rec.id) LOOP
            v_suffix := v_suffix + 1;
            v_final := 'user_' || substr(p_rec.id::text, 1, 6) || '_' || v_suffix::text;
        END LOOP;
        
        UPDATE public.profiles
        SET username = v_final, is_private = false, updated_at = now()
        WHERE id = p_rec.id;
    END LOOP;
END $$;

-- 5. החזרת האילוצים והאינדקס הייחודי (כעת אין שום כפילויות בעולם!)
ALTER TABLE public.profiles
    ADD CONSTRAINT chk_username_format
    CHECK (username IS NULL OR username ~ '^[a-zA-Z0-9_]{3,30}$');

CREATE UNIQUE INDEX IF NOT EXISTS uq_profiles_username_lower
    ON public.profiles(LOWER(username))
    WHERE username IS NOT NULL AND username <> '';

-- 6. מדיניות אבטחה RLS - כולם רואים את כולם באפליקציה
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public profiles are viewable by authenticated users" ON public.profiles;
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can view all profiles" ON public.profiles;
DROP POLICY IF EXISTS "Profiles viewable by everyone" ON public.profiles;
DROP POLICY IF EXISTS "Public profiles viewable by anon" ON public.profiles;

CREATE POLICY "Public profiles are viewable by authenticated users" ON public.profiles
    FOR SELECT TO authenticated USING (true);

CREATE POLICY "Public profiles viewable by anon" ON public.profiles
    FOR SELECT TO anon USING (true);

DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;
CREATE POLICY "Users can insert their own profile" ON public.profiles
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
CREATE POLICY "Users can update their own profile" ON public.profiles
    FOR UPDATE TO authenticated USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- RLS לטבלת חברים ובקשות חברות
ALTER TABLE public.friends ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their friends" ON public.friends;
CREATE POLICY "Users can view their friends" ON public.friends
    FOR SELECT TO authenticated USING (auth.uid() = user_id OR auth.uid() = friend_id);

DROP POLICY IF EXISTS "Users can insert friends" ON public.friends;
CREATE POLICY "Users can insert friends" ON public.friends
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete friends" ON public.friends;
CREATE POLICY "Users can delete friends" ON public.friends
    FOR DELETE TO authenticated USING (auth.uid() = user_id OR auth.uid() = friend_id);

ALTER TABLE public.friend_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their requests" ON public.friend_requests;
CREATE POLICY "Users can view their requests" ON public.friend_requests
    FOR SELECT TO authenticated USING (auth.uid() = from_user_id OR auth.uid() = to_user_id);

DROP POLICY IF EXISTS "Users can create requests" ON public.friend_requests;
CREATE POLICY "Users can create requests" ON public.friend_requests
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = from_user_id);

DROP POLICY IF EXISTS "Users can update their received requests" ON public.friend_requests;
CREATE POLICY "Users can update their received requests" ON public.friend_requests
    FOR UPDATE TO authenticated USING (auth.uid() = to_user_id);

-- 7. טריגרים אוטומטיים להרשמות עתידיות
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

CREATE OR REPLACE FUNCTION public.handle_new_user_sync()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_candidate TEXT;
    v_final TEXT;
    v_suffix INT;
    v_full_name TEXT;
    v_display_name TEXT;
BEGIN
    v_candidate := lower(trim(leading '@' from trim(COALESCE(
        NEW.raw_user_meta_data->>'username',
        NEW.raw_user_meta_data->>'user_name',
        ''
    ))));

    IF v_candidate = '' THEN
        IF NEW.email IS NOT NULL AND NEW.email NOT LIKE '%@shorashim.app' THEN
            v_candidate := lower(split_part(NEW.email, '@', 1));
        ELSE
            v_candidate := 'user_' || substr(NEW.id::text, 1, 8);
        END IF;
    END IF;

    -- ניקוי תווים
    v_candidate := regexp_replace(v_candidate, '[^a-z0-9_]', '_', 'g');
    v_candidate := trim(both '_' from v_candidate);
    IF length(v_candidate) < 3 THEN
        v_candidate := 'user_' || substr(NEW.id::text, 1, 6);
    END IF;
    IF length(v_candidate) > 22 THEN
        v_candidate := substr(v_candidate, 1, 22);
    END IF;

    -- מניעת כפילות מול משתמשים קיימים
    v_final := v_candidate;
    v_suffix := 1;
    WHILE EXISTS (
        SELECT 1 FROM public.profiles 
        WHERE lower(username) = v_final 
          AND id <> NEW.id
    ) LOOP
        v_suffix := v_suffix + 1;
        v_final := substr(v_candidate, 1, 20) || '_' || v_suffix::text;
    END LOOP;

    v_full_name := COALESCE(
        NULLIF(NEW.raw_user_meta_data->>'full_name', ''), 
        NULLIF(NEW.raw_user_meta_data->>'display_name', ''), 
        v_final,
        'משתמש'
    );
    v_display_name := COALESCE(
        NULLIF(NEW.raw_user_meta_data->>'display_name', ''), 
        v_full_name
    );

    INSERT INTO public.profiles (
        id,
        username,
        full_name,
        display_name,
        email,
        is_private,
        created_at,
        updated_at
    ) VALUES (
        NEW.id,
        v_final,
        v_full_name,
        v_display_name,
        NEW.email,
        false,
        now(),
        now()
    )
    ON CONFLICT (id) DO UPDATE SET
        username = v_final,
        full_name = COALESCE(NULLIF(public.profiles.full_name, ''), EXCLUDED.full_name),
        display_name = COALESCE(NULLIF(public.profiles.display_name, ''), EXCLUDED.display_name),
        email = COALESCE(public.profiles.email, EXCLUDED.email),
        is_private = false,
        updated_at = now();

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created_sync ON auth.users;
DROP TRIGGER IF EXISTS on_auth_user_created_after ON auth.users;

CREATE TRIGGER on_auth_user_created_after
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user_sync();

-- 8. פונקציות RPC להצגת וחיפוש משתמשים
DROP FUNCTION IF EXISTS public.get_community_users(INT);
DROP FUNCTION IF EXISTS public.get_community_users();

CREATE OR REPLACE FUNCTION public.get_community_users(p_limit INT DEFAULT 100)
RETURNS TABLE (
    id UUID,
    username TEXT,
    display_name TEXT,
    avatar_url TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_me UUID := auth.uid();
BEGIN
    RETURN QUERY
    SELECT 
        p.id,
        p.username,
        COALESCE(NULLIF(p.display_name, ''), NULLIF(p.full_name, ''), p.username, 'משתמש') AS display_name,
        p.avatar_url
    FROM public.profiles p
    WHERE (v_me IS NULL OR p.id <> v_me)
      AND (p.is_private IS FALSE OR p.is_private IS NULL)
      AND p.username IS NOT NULL
      AND TRIM(p.username) <> ''
    ORDER BY p.updated_at DESC NULLS LAST
    LIMIT p_limit;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_community_users(INT) TO authenticated, anon;

DROP FUNCTION IF EXISTS public.search_app_users(TEXT, INT);
DROP FUNCTION IF EXISTS public.search_app_users(TEXT);

CREATE OR REPLACE FUNCTION public.search_app_users(p_query TEXT, p_limit INT DEFAULT 50)
RETURNS TABLE (
    id UUID,
    username TEXT,
    full_name TEXT,
    display_name TEXT,
    avatar_url TEXT,
    is_private BOOLEAN
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_clean TEXT := LOWER(TRIM(LEADING '@' FROM TRIM(COALESCE(p_query, ''))));
    v_me UUID := auth.uid();
BEGIN
    IF v_clean = '' THEN
        RETURN;
    END IF;

    RETURN QUERY
    SELECT 
        p.id,
        p.username,
        COALESCE(p.full_name, '') AS full_name,
        COALESCE(NULLIF(p.display_name, ''), NULLIF(p.full_name, ''), p.username, 'משתמש') AS display_name,
        p.avatar_url,
        COALESCE(p.is_private, false) AS is_private
    FROM public.profiles p
    WHERE (v_me IS NULL OR p.id <> v_me)
      AND (
          LOWER(COALESCE(p.username, '')) LIKE '%' || v_clean || '%'
          OR LOWER(COALESCE(p.display_name, '')) LIKE '%' || v_clean || '%'
          OR LOWER(COALESCE(p.full_name, '')) LIKE '%' || v_clean || '%'
      )
    ORDER BY 
        CASE WHEN LOWER(p.username) = v_clean THEN 0
             WHEN LOWER(p.username) LIKE v_clean || '%' THEN 1
             ELSE 2 END,
        p.updated_at DESC NULLS LAST
    LIMIT p_limit;
END;
$$;

GRANT EXECUTE ON FUNCTION public.search_app_users(TEXT, INT) TO authenticated, anon;

-- 9. פונקציות הוספת וניהול חברים
DROP FUNCTION IF EXISTS public.send_friend_request(TEXT);

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

    -- חיבור הדדי מיידי
    INSERT INTO public.friend_requests (from_user_id, to_user_id, status)
    VALUES (v_me, v_target, 'ACCEPTED')
    ON CONFLICT (from_user_id, to_user_id)
    DO UPDATE SET status = 'ACCEPTED', updated_at = now()
    RETURNING id INTO v_req_id;

    INSERT INTO public.friends (user_id, friend_id) VALUES (v_me, v_target) ON CONFLICT DO NOTHING;
    INSERT INTO public.friends (user_id, friend_id) VALUES (v_target, v_me) ON CONFLICT DO NOTHING;

    RETURN jsonb_build_object('ok', true, 'connected', true, 'user_id', v_target);
END;
$$;

GRANT EXECUTE ON FUNCTION public.send_friend_request(TEXT) TO authenticated;

DROP FUNCTION IF EXISTS public.respond_friend_request(UUID, BOOLEAN);

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

GRANT EXECUTE ON FUNCTION public.respond_friend_request(UUID, BOOLEAN) TO authenticated;

DROP FUNCTION IF EXISTS public.get_my_friend_requests();

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
    SELECT 
        r.id, 
        r.from_user_id, 
        p.username, 
        COALESCE(NULLIF(p.display_name, ''), NULLIF(p.full_name, ''), p.username, 'משתמש') AS display_name, 
        COALESCE(p.full_name, p.username, 'משתמש') AS full_name, 
        p.avatar_url, 
        r.created_at
    FROM public.friend_requests r
    JOIN public.profiles p ON p.id = r.from_user_id
    WHERE r.to_user_id = auth.uid() AND r.status = 'PENDING'
    ORDER BY r.created_at DESC;
$$;

GRANT EXECUTE ON FUNCTION public.get_my_friend_requests() TO authenticated;

DROP FUNCTION IF EXISTS public.remove_friend(UUID);

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

GRANT EXECUTE ON FUNCTION public.remove_friend(UUID) TO authenticated;

-- 10. פונקציות התחברות ובדיקת זמינות שמות משתמש
DROP FUNCTION IF EXISTS public.check_username_available(TEXT);

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

    IF EXISTS (SELECT 1 FROM public.profiles WHERE lower(username) = v_clean) THEN
        RETURN FALSE;
    END IF;

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

DROP FUNCTION IF EXISTS public.resolve_login_email(TEXT);

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

    IF v_clean ~ '@' THEN
        SELECT email INTO v_email
        FROM auth.users
        WHERE lower(email) = v_clean
        LIMIT 1;

        IF v_email IS NOT NULL THEN
            RETURN v_email;
        END IF;

        SELECT u.email INTO v_email
        FROM public.profiles p
        JOIN auth.users u ON u.id = p.id
        WHERE lower(p.email) = v_clean
        LIMIT 1;

        IF v_email IS NOT NULL THEN
            RETURN v_email;
        END IF;
    END IF;

    SELECT u.email INTO v_email
    FROM public.profiles p
    JOIN auth.users u ON u.id = p.id
    WHERE lower(p.username) = v_clean
    LIMIT 1;

    IF v_email IS NOT NULL THEN
        RETURN v_email;
    END IF;

    SELECT email INTO v_email
    FROM auth.users
    WHERE lower(COALESCE(raw_user_meta_data->>'username', '')) = v_clean
    LIMIT 1;

    IF v_email IS NOT NULL THEN
        RETURN v_email;
    END IF;

    IF v_clean !~ '@' THEN
        RETURN v_clean || '@shorashim.app';
    END IF;

    RETURN v_clean;
END;
$$;

GRANT EXECUTE ON FUNCTION public.resolve_login_email(TEXT) TO anon, authenticated;

DROP FUNCTION IF EXISTS public.set_my_username(TEXT);

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

    UPDATE public.profiles
    SET username = v_clean,
        updated_at = NOW()
    WHERE id = v_user_id;

    UPDATE auth.users
    SET raw_user_meta_data = COALESCE(raw_user_meta_data, '{}'::jsonb) || jsonb_build_object('username', v_clean)
    WHERE id = v_user_id;

    RETURN jsonb_build_object('ok', true, 'username', v_clean);
END;
$$;

GRANT EXECUTE ON FUNCTION public.set_my_username(TEXT) TO authenticated;
