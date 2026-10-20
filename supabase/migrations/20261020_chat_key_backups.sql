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
