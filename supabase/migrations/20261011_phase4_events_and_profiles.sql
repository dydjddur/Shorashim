-- ==============================================================================
-- Shorashim (שורשים) — Phase 4 Database Schema & Security Migration
-- Tables: memorial_events, profiles
-- Features: Memorial Events Management, RBAC, User Profiles
-- ==============================================================================

-- 1. PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT '',
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_profiles_updated_at ON public.profiles(updated_at);

-- Profiles RLS
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

-- 2. MEMORIAL EVENTS TABLE
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

-- Memorial Events RLS
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
