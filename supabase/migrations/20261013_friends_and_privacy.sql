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
