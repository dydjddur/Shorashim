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
