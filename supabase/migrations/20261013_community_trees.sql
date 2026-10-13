-- ==============================================================================
-- Shorashim (שורשים) — Phase 7: Community / Public Shared Family Trees
-- Fix: Prevent infinite recursion in PostgreSQL RLS policies
-- Uses SECURITY DEFINER helper functions to bypass RLS evaluation loops.
-- ==============================================================================

-- 1. Index on people.last_name for fast surname discovery
CREATE INDEX IF NOT EXISTS idx_people_last_name ON public.people(last_name);

-- 2. Ensure all existing trees are marked as PUBLISHED so community can view them
-- NOTE: only rows without a status are touched. Trees an owner made private (status DRAFT or PRIVATE) must stay
-- private even if this migration is re-run, otherwise "make private" would silently be undone.
UPDATE public.families
SET status = 'PUBLISHED'
WHERE status IS NULL;

-- 3. Update create_family function so new trees are automatically PUBLISHED
CREATE OR REPLACE FUNCTION public.create_family(p_name TEXT, p_description TEXT DEFAULT NULL)
RETURNS public.families
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID;
    v_new_family public.families;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF char_length(trim(p_name)) < 2 THEN
        RAISE EXCEPTION 'Family name must be at least 2 characters';
    END IF;

    -- Created in PUBLISHED status so all community members can see it immediately
    INSERT INTO public.families (name, description, owner_id, status)
    VALUES (trim(p_name), trim(p_description), v_user_id, 'PUBLISHED')
    RETURNING * INTO v_new_family;

    -- Add creator as OWNER
    INSERT INTO public.family_members (family_id, user_id, role)
    VALUES (v_new_family.id, v_user_id, 'OWNER');

    RETURN v_new_family;
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_family(TEXT, TEXT) TO authenticated, anon;

-- 4. SECURITY DEFINER Helper Functions (Prevents RLS Recursion)
CREATE OR REPLACE FUNCTION public.is_family_member(p_family_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.family_members
        WHERE family_id = p_family_id AND user_id = p_user_id
    );
$$;

CREATE OR REPLACE FUNCTION public.is_family_published(p_family_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.families
        WHERE id = p_family_id AND status = 'PUBLISHED'
    );
$$;

GRANT EXECUTE ON FUNCTION public.is_family_member(UUID, UUID) TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.is_family_published(UUID) TO authenticated, anon;

-- 5. Row Level Security on families:
DROP POLICY IF EXISTS "Members can view their families" ON public.families;
DROP POLICY IF EXISTS "Users can view their families or published families" ON public.families;

CREATE POLICY "Users can view their families or published families" ON public.families
    FOR SELECT TO authenticated, anon USING (
        status = 'PUBLISHED'
        OR owner_id = auth.uid()
        OR public.is_family_member(id, auth.uid())
    );

-- 6. Row Level Security on family_members:
DROP POLICY IF EXISTS "Members can view co-members" ON public.family_members;
DROP POLICY IF EXISTS "Users can view co-members or members of published families" ON public.family_members;

CREATE POLICY "Users can view co-members or members of published families" ON public.family_members
    FOR SELECT TO authenticated, anon USING (
        user_id = auth.uid()
        OR public.is_family_published(family_id)
        OR public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'])
    );

-- 7. Row Level Security on people:
DROP POLICY IF EXISTS "Members can view people" ON public.people;
DROP POLICY IF EXISTS "Users can view people in their families or published families" ON public.people;

CREATE POLICY "Users can view people in their families or published families" ON public.people
    FOR SELECT TO authenticated, anon USING (
        public.is_family_published(family_id)
        OR public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'])
    );

-- 8. Row Level Security on relationships:
DROP POLICY IF EXISTS "Members can view relationships" ON public.relationships;
DROP POLICY IF EXISTS "Users can view relationships in their families or published families" ON public.relationships;

CREATE POLICY "Users can view relationships in their families or published families" ON public.relationships
    FOR SELECT TO authenticated, anon USING (
        public.is_family_published(family_id)
        OR public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'])
    );

-- 9. RPC Function: get_public_trees_summary
-- Aggregates published trees with their person count and distinct list of surnames.
CREATE OR REPLACE FUNCTION public.get_public_trees_summary()
RETURNS TABLE (
    id UUID,
    name TEXT,
    description TEXT,
    owner_id UUID,
    owner_name TEXT,
    status TEXT,
    person_count BIGINT,
    surnames TEXT[],
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
    SELECT
        f.id,
        f.name,
        f.description,
        f.owner_id,
        COALESCE(NULLIF(p.display_name, ''), NULLIF(p.full_name, ''), 'יוצר העץ') AS owner_name,
        f.status,
        COUNT(pe.id) AS person_count,
        COALESCE(
            ARRAY_REMOVE(ARRAY_AGG(DISTINCT TRIM(pe.last_name)) FILTER (WHERE pe.last_name IS NOT NULL AND TRIM(pe.last_name) <> ''), NULL),
            ARRAY[]::TEXT[]
        ) AS surnames,
        f.created_at,
        f.updated_at
    FROM public.families f
    LEFT JOIN public.profiles p ON p.id = f.owner_id
    LEFT JOIN public.people pe ON pe.family_id = f.id
    WHERE f.status = 'PUBLISHED'
    GROUP BY f.id, f.name, f.description, f.owner_id, p.display_name, p.full_name, f.status, f.created_at, f.updated_at
    ORDER BY f.updated_at DESC;
$$;

GRANT EXECUTE ON FUNCTION public.get_public_trees_summary() TO authenticated, anon;
