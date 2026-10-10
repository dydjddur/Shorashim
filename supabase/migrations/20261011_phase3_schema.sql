-- ==============================================================================
-- Shorashim (שורשים) — Phase 3 Database Schema & Security Migration
-- Tables: families, family_members, people, relationships
-- Features: Draft & Publish Lifecycle, Granular RBAC, Interactive Workflow Map
-- ==============================================================================

-- 1. FAMILIES TABLE
CREATE TABLE IF NOT EXISTS public.families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    schema_version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_family_name_length CHECK (char_length(trim(name)) >= 2)
);

CREATE INDEX IF NOT EXISTS idx_families_owner_id ON public.families(owner_id);
CREATE INDEX IF NOT EXISTS idx_families_status ON public.families(status);

-- 2. FAMILY MEMBERS TABLE (Role-Based Access Control)
-- Roles:
-- OWNER: Tree creator (super-admin), full control, can delete tree and appoint admins.
-- ADMIN: Co-manager, can invite members, edit tree metadata, and manage content.
-- CONTRIBUTOR: Regular family member, can add/edit relatives and relationships.
-- VIEWER: Read-only access.
CREATE TABLE IF NOT EXISTS public.family_members (
    family_id UUID NOT NULL REFERENCES public.families(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'CONTRIBUTOR' CHECK (role IN ('OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER')),
    invited_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (family_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_family_members_user_id ON public.family_members(user_id);
CREATE INDEX IF NOT EXISTS idx_family_members_role ON public.family_members(role);

-- 3. PEOPLE TABLE (With Workflow Map Canvas Coordinates)
CREATE TABLE IF NOT EXISTS public.people (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES public.families(id) ON DELETE CASCADE,
    first_name TEXT NOT NULL,
    last_name TEXT,
    additional_names TEXT,
    photo_path TEXT,
    life_status TEXT NOT NULL CHECK (life_status IN ('LIVING', 'DECEASED')),
    birth_date DATE,
    birth_date_precision TEXT CHECK (birth_date_precision IN ('FULL_DATE', 'YEAR_MONTH', 'YEAR_ONLY', 'UNKNOWN')),
    death_date DATE,
    death_date_precision TEXT CHECK (death_date_precision IN ('FULL_DATE', 'YEAR_MONTH', 'YEAR_ONLY', 'UNKNOWN')),
    biography TEXT,
    canvas_x FLOAT NOT NULL DEFAULT 0.0,
    canvas_y FLOAT NOT NULL DEFAULT 0.0,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_person_first_name_not_empty CHECK (char_length(trim(first_name)) >= 1),
    CONSTRAINT chk_living_person_no_death CHECK (
        (life_status = 'LIVING' AND death_date IS NULL AND death_date_precision IS NULL)
        OR (life_status = 'DECEASED')
    )
);

CREATE INDEX IF NOT EXISTS idx_people_family_id ON public.people(family_id);
CREATE INDEX IF NOT EXISTS idx_people_names ON public.people(family_id, first_name, last_name);

-- 4. RELATIONSHIPS TABLE
CREATE TABLE IF NOT EXISTS public.relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES public.families(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES public.people(id) ON DELETE CASCADE,
    related_person_id UUID NOT NULL REFERENCES public.people(id) ON DELETE CASCADE,
    relationship_type TEXT NOT NULL CHECK (relationship_type IN ('PARENT_CHILD', 'SPOUSE')),
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_no_self_relationship CHECK (person_id <> related_person_id)
);

CREATE INDEX IF NOT EXISTS idx_relationships_family_id ON public.relationships(family_id);
CREATE INDEX IF NOT EXISTS idx_relationships_person_id ON public.relationships(person_id);
CREATE INDEX IF NOT EXISTS idx_relationships_related_person ON public.relationships(related_person_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_parent_child ON public.relationships(family_id, person_id, related_person_id)
WHERE relationship_type = 'PARENT_CHILD';

CREATE UNIQUE INDEX IF NOT EXISTS uq_spouse_undirected ON public.relationships(
    family_id,
    LEAST(person_id, related_person_id),
    GREATEST(person_id, related_person_id)
)
WHERE relationship_type = 'SPOUSE';

-- 5. ROW LEVEL SECURITY (RLS)
ALTER TABLE public.families ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.family_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.people ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.relationships ENABLE ROW LEVEL SECURITY;

-- Helper function to check if current user is at least a specified role in a family
CREATE OR REPLACE FUNCTION public.has_family_role(p_family_id UUID, p_required_roles TEXT[])
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.family_members
        WHERE family_id = p_family_id
          AND user_id = auth.uid()
          AND role = ANY(p_required_roles)
    ) OR EXISTS (
        SELECT 1 FROM public.families
        WHERE id = p_family_id
          AND owner_id = auth.uid()
    );
$$;

-- Families Policies
DROP POLICY IF EXISTS "Members can view their families" ON public.families;
CREATE POLICY "Members can view their families" ON public.families
    FOR SELECT USING (
        owner_id = auth.uid() OR
        id IN (SELECT family_id FROM public.family_members WHERE user_id = auth.uid())
    );

DROP POLICY IF EXISTS "Owners and admins can update family" ON public.families;
CREATE POLICY "Owners and admins can update family" ON public.families
    FOR UPDATE USING (
        public.has_family_role(id, ARRAY['OWNER', 'ADMIN'])
    ) WITH CHECK (
        public.has_family_role(id, ARRAY['OWNER', 'ADMIN'])
    );

DROP POLICY IF EXISTS "Only owners can delete family" ON public.families;
CREATE POLICY "Only owners can delete family" ON public.families
    FOR DELETE USING (owner_id = auth.uid());

-- Family Members Policies
DROP POLICY IF EXISTS "Members can view co-members" ON public.family_members;
CREATE POLICY "Members can view co-members" ON public.family_members
    FOR SELECT USING (
        user_id = auth.uid() OR
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'])
    );

DROP POLICY IF EXISTS "Owners and admins can manage members" ON public.family_members;
CREATE POLICY "Owners and admins can manage members" ON public.family_members
    FOR ALL USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN'])
    );

-- People Policies
DROP POLICY IF EXISTS "Members can view people" ON public.people;
CREATE POLICY "Members can view people" ON public.people
    FOR SELECT USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can insert people" ON public.people;
CREATE POLICY "Contributors, admins, owners can insert people" ON public.people
    FOR INSERT WITH CHECK (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can update people" ON public.people;
CREATE POLICY "Contributors, admins, owners can update people" ON public.people
    FOR UPDATE USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    ) WITH CHECK (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can delete people" ON public.people;
CREATE POLICY "Contributors, admins, owners can delete people" ON public.people
    FOR DELETE USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

-- Relationships Policies
DROP POLICY IF EXISTS "Members can view relationships" ON public.relationships;
CREATE POLICY "Members can view relationships" ON public.relationships
    FOR SELECT USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can insert relationships" ON public.relationships;
CREATE POLICY "Contributors, admins, owners can insert relationships" ON public.relationships
    FOR INSERT WITH CHECK (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

DROP POLICY IF EXISTS "Contributors, admins, owners can delete relationships" ON public.relationships;
CREATE POLICY "Contributors, admins, owners can delete relationships" ON public.relationships
    FOR DELETE USING (
        public.has_family_role(family_id, ARRAY['OWNER', 'ADMIN', 'CONTRIBUTOR'])
    );

-- 6. ATOMIC FAMILY CREATION FUNCTION (Initial Status: DRAFT)
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

    -- Created in DRAFT status
    INSERT INTO public.families (name, description, owner_id, status)
    VALUES (trim(p_name), trim(p_description), v_user_id, 'DRAFT')
    RETURNING * INTO v_new_family;

    -- Add creator as OWNER
    INSERT INTO public.family_members (family_id, user_id, role)
    VALUES (v_new_family.id, v_user_id, 'OWNER');

    RETURN v_new_family;
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_family(TEXT, TEXT) TO authenticated;

-- Direct insert policy for families table fallback
DROP POLICY IF EXISTS "Authenticated users can create draft families" ON public.families;
CREATE POLICY "Authenticated users can create draft families" ON public.families
    FOR INSERT WITH CHECK (auth.uid() = owner_id);

-- 7. PARENT-CHILD CYCLE CHECK TRIGGER
CREATE OR REPLACE FUNCTION public.validate_relationship_integrity()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_p1_family UUID;
    v_p2_family UUID;
    v_cycle_detected BOOLEAN;
BEGIN
    SELECT family_id INTO v_p1_family FROM public.people WHERE id = NEW.person_id;
    SELECT family_id INTO v_p2_family FROM public.people WHERE id = NEW.related_person_id;

    IF v_p1_family IS NULL OR v_p2_family IS NULL THEN
        RAISE EXCEPTION 'One or both people do not exist';
    END IF;

    IF v_p1_family <> NEW.family_id OR v_p2_family <> NEW.family_id THEN
        RAISE EXCEPTION 'Both individuals must belong to the specified family';
    END IF;

    IF NEW.relationship_type = 'PARENT_CHILD' THEN
        WITH RECURSIVE descendant_tree AS (
            SELECT related_person_id AS person
            FROM public.relationships
            WHERE person_id = NEW.related_person_id
              AND relationship_type = 'PARENT_CHILD'
            UNION
            SELECT r.related_person_id
            FROM public.relationships r
            INNER JOIN descendant_tree dt ON r.person_id = dt.person
            WHERE r.relationship_type = 'PARENT_CHILD'
        )
        SELECT EXISTS (
            SELECT 1 FROM descendant_tree WHERE person = NEW.person_id
        ) INTO v_cycle_detected;

        IF v_cycle_detected THEN
            RAISE EXCEPTION 'Parent-child relationship creates a circular ancestry loop';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validate_relationship ON public.relationships;
CREATE TRIGGER trg_validate_relationship
    BEFORE INSERT OR UPDATE ON public.relationships
    FOR EACH ROW
    EXECUTE FUNCTION public.validate_relationship_integrity();
