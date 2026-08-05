-- SUPABASE SCHEMAS AND MIGRATIONS FOR BREW STUDIO / TALLER DEL BREWTHER
-- File: supabase/migrations/001_brew_studio_schema.sql

-- Enable clean UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table: profiles (Extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    display_name TEXT NOT NULL,
    handle TEXT UNIQUE,
    avatar_url TEXT,
    avatar_color TEXT DEFAULT '#3F7A63',
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'admin', 'moderator')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: beans
CREATE TABLE IF NOT EXISTS public.beans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    roaster TEXT,
    name TEXT NOT NULL,
    origin TEXT,
    altitude TEXT,
    process TEXT,
    roast_date TEXT,
    first_use_date TEXT,
    notes TEXT,
    status TEXT DEFAULT 'cerrado' CHECK (status IN ('cerrado', 'abierto', 'terminado')),
    stock_grams NUMERIC DEFAULT 250,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: grinders
CREATE TABLE IF NOT EXISTS public.grinders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    brand TEXT NOT NULL,
    model TEXT NOT NULL,
    click_range TEXT,
    favorite_clicks_by_method TEXT,
    calibration_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: equipment
CREATE TABLE IF NOT EXISTS public.equipment (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('molino', 'método', 'báscula', 'tetera', 'filtros', 'servidor', 'prensa', 'accesorios', 'otro')),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: brew_methods
CREATE TABLE IF NOT EXISTS public.brew_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT UNIQUE NOT NULL,
    standard_ratio NUMERIC DEFAULT 16.0,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: techniques
CREATE TABLE IF NOT EXISTS public.techniques (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL,
    owner_display_name TEXT,
    name TEXT NOT NULL CHECK (char_length(name) <= 120),
    method TEXT,
    coffee_grams NUMERIC,
    water_ml INTEGER,
    ratio NUMERIC,
    temperature INTEGER,
    grind_clicks TEXT,
    grinder_id UUID REFERENCES public.grinders(id) ON DELETE SET NULL,
    bean_id UUID REFERENCES public.beans(id) ON DELETE SET NULL,
    notes TEXT CHECK (char_length(notes) <= 2000),
    visibility TEXT DEFAULT 'private' CHECK (visibility IN ('private', 'public', 'unlisted')),
    is_shared BOOLEAN DEFAULT FALSE,
    original_author_user_id UUID,
    original_author_name TEXT,
    original_entity_id UUID,
    imported_from_share_id UUID,
    copy_mode TEXT DEFAULT 'original' CHECK (copy_mode IN ('original', 'fork')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: technique_steps
CREATE TABLE IF NOT EXISTS public.technique_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    technique_id UUID NOT NULL REFERENCES public.techniques(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    title TEXT,
    duration_sec INTEGER DEFAULT 0,
    water_add_ml INTEGER DEFAULT 0,
    target_water_ml INTEGER DEFAULT 0,
    gesture TEXT,
    intensity TEXT,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: recipes
CREATE TABLE IF NOT EXISTS public.recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL,
    owner_display_name TEXT,
    name TEXT NOT NULL CHECK (char_length(name) <= 120),
    method TEXT,
    bean_id UUID REFERENCES public.beans(id) ON DELETE SET NULL,
    grinder_id UUID REFERENCES public.grinders(id) ON DELETE SET NULL,
    technique_id UUID REFERENCES public.techniques(id) ON DELETE SET NULL,
    coffee_grams NUMERIC,
    water_ml INTEGER,
    ratio NUMERIC,
    temperature INTEGER,
    clicks TEXT,
    notes TEXT CHECK (char_length(notes) <= 2000),
    visibility TEXT DEFAULT 'private' CHECK (visibility IN ('private', 'public', 'unlisted')),
    is_shared BOOLEAN DEFAULT FALSE,
    original_author_user_id UUID,
    original_author_name TEXT,
    original_entity_id UUID,
    imported_from_share_id UUID,
    copy_mode TEXT DEFAULT 'original' CHECK (copy_mode IN ('original', 'fork')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: cups
CREATE TABLE IF NOT EXISTS public.cups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    method TEXT NOT NULL,
    technique_name TEXT,
    recipe_name TEXT,
    bean_name TEXT,
    grinder_name TEXT,
    clicks INTEGER,
    coffee_grams NUMERIC,
    water_ml INTEGER,
    ratio NUMERIC,
    temperature INTEGER,
    duration_seconds INTEGER,
    cup_life_state TEXT,
    expected_notes TEXT,
    found_notes TEXT,
    texture TEXT,
    cleanliness TEXT,
    persistence TEXT,
    rating NUMERIC CHECK (rating >= 1.0 AND rating <= 5.0),
    free_notes TEXT,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: cupping_notes
CREATE TABLE IF NOT EXISTS public.cupping_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cup_id UUID REFERENCES public.cups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    descriptor TEXT NOT NULL,
    intensity INTEGER DEFAULT 3 CHECK (intensity >= 1 AND intensity <= 5),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: lab_experiments
CREATE TABLE IF NOT EXISTS public.lab_experiments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    method_name TEXT NOT NULL,
    coffee_grams NUMERIC,
    water_ml INTEGER,
    ratio NUMERIC,
    temperature INTEGER,
    grinder_name TEXT,
    clicks INTEGER,
    bean_name TEXT,
    bean_freshness TEXT,
    estimated_time_seconds INTEGER,
    experiment_notes TEXT,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: shares
CREATE TABLE IF NOT EXISTS public.shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type TEXT NOT NULL CHECK (entity_type IN ('recipe', 'technique')),
    entity_id UUID NOT NULL,
    from_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    from_name TEXT NOT NULL,
    from_handle TEXT,
    target_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    visibility TEXT NOT NULL DEFAULT 'public' CHECK (visibility IN ('public', 'direct', 'unlisted')),
    name TEXT NOT NULL,
    subtitle TEXT,
    message TEXT CHECK (char_length(message) <= 280),
    payload_snapshot_json JSONB NOT NULL,
    original_author_user_id UUID,
    original_author_name TEXT,
    original_entity_id UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: share_likes
CREATE TABLE IF NOT EXISTS public.share_likes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    share_id UUID NOT NULL REFERENCES public.shares(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(share_id, user_id)
);

-- Table: share_saves
CREATE TABLE IF NOT EXISTS public.share_saves (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    share_id UUID NOT NULL REFERENCES public.shares(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(share_id, user_id)
);

-- Table: inbox_items
CREATE TABLE IF NOT EXISTS public.inbox_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    share_id UUID NOT NULL REFERENCES public.shares(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: activity_log
CREATE TABLE IF NOT EXISTS public.activity_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id UUID,
    share_id UUID REFERENCES public.shares(id) ON DELETE SET NULL,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);


-- INDEXES FOR HIGH-PERFORMANCE RETRIEVALS
CREATE INDEX IF NOT EXISTS idx_shares_from_user ON public.shares(from_user_id);
CREATE INDEX IF NOT EXISTS idx_shares_visibility ON public.shares(visibility);
CREATE INDEX IF NOT EXISTS idx_share_likes_share ON public.share_likes(share_id);
CREATE INDEX IF NOT EXISTS idx_share_saves_user ON public.share_saves(user_id);
CREATE INDEX IF NOT EXISTS idx_inbox_target_user ON public.inbox_items(target_user_id);
CREATE INDEX IF NOT EXISTS idx_activity_user ON public.activity_log(user_id);
CREATE INDEX IF NOT EXISTS idx_recipes_user ON public.recipes(user_id);
CREATE INDEX IF NOT EXISTS idx_techniques_user ON public.techniques(user_id);
CREATE INDEX IF NOT EXISTS idx_steps_technique ON public.technique_steps(technique_id);


-- UTILITY AUTOMATION TRIGGERS

-- Trigger function: Update timestamp
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply timestamp update to tables
CREATE TRIGGER update_profiles_modtime BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_beans_modtime BEFORE UPDATE ON public.beans FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_grinders_modtime BEFORE UPDATE ON public.grinders FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_equipment_modtime BEFORE UPDATE ON public.equipment FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_techniques_modtime BEFORE UPDATE ON public.techniques FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_recipes_modtime BEFORE UPDATE ON public.recipes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_shares_modtime BEFORE UPDATE ON public.shares FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- Action: New user signups automatically generate profiles
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    fallback_name TEXT;
    at_pos INTEGER;
    new_handle TEXT;
BEGIN
    -- Extract visual nickname from email as generic starting handle
    at_pos := POSITION('@' IN NEW.email);
    IF at_pos > 1 THEN
        fallback_name := SUBSTRING(NEW.email FROM 1 FOR at_pos - 1);
    ELSE
        fallback_name := 'barista_' || SUBSTRING(NEW.id::TEXT FROM 1 FOR 6);
    END IF;
    
    new_handle := LOWER(fallback_name) || '_' || SUBSTRING(NEW.id::TEXT FROM 1 FOR 4);

    INSERT INTO public.profiles (id, email, display_name, handle, avatar_url, avatar_color, role)
    VALUES (
        NEW.id,
        NEW.email,
        fallback_name,
        new_handle,
        NULL,
        '#3F7A63',
        'user'
    )
    ON CONFLICT (id) DO NOTHING;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Map handle_new_user to Supabase auth.users insert
CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- ROW LEVEL SECURITY (RLS) POLICIES

-- Profiles RLS
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public profiles are readable by everyone" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Users can update their own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Beans RLS
ALTER TABLE public.beans ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can select their own beans" ON public.beans FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own beans" ON public.beans FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own beans" ON public.beans FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own beans" ON public.beans FOR DELETE USING (auth.uid() = user_id);

-- Grinders RLS
ALTER TABLE public.grinders ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Grinders are client specific" ON public.grinders FOR ALL USING (auth.uid() = user_id);

-- Equipment RLS
ALTER TABLE public.equipment ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Equipment is client specific" ON public.equipment FOR ALL USING (auth.uid() = user_id);

-- Recipes RLS
ALTER TABLE public.recipes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Owners can CRUD recipes" ON public.recipes FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Public recipes are visible to everyone" ON public.recipes FOR SELECT USING (visibility = 'public');

-- Techniques RLS
ALTER TABLE public.techniques ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Owners can CRUD techniques" ON public.techniques FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Public techniques are visible to everyone" ON public.techniques FOR SELECT USING (visibility = 'public');

-- Technique Steps RLS
ALTER TABLE public.technique_steps ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Steps are readable if technique is accessible" ON public.technique_steps FOR SELECT USING (
    auth.uid() = user_id OR 
    EXISTS (SELECT 1 FROM public.techniques WHERE id = technique_id AND visibility = 'public')
);
CREATE POLICY "Owners can manage steps" ON public.technique_steps FOR ALL USING (auth.uid() = user_id);

-- Cups RLS
ALTER TABLE public.cups ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Cups are client specific" ON public.cups FOR ALL USING (auth.uid() = user_id);

-- Cupping Notes RLS
ALTER TABLE public.cupping_notes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Cupping notes are client specific" ON public.cupping_notes FOR ALL USING (auth.uid() = user_id);

-- Lab Experiments RLS
ALTER TABLE public.lab_experiments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Lab experiments are private" ON public.lab_experiments FOR ALL USING (auth.uid() = user_id);

-- Shares RLS
ALTER TABLE public.shares ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public shares are visible to all users" ON public.shares FOR SELECT USING (
    visibility = 'public' OR 
    auth.uid() = from_user_id OR 
    auth.uid() = target_user_id
);
CREATE POLICY "Owners can write shares of owned entities" ON public.shares FOR INSERT WITH CHECK (
    auth.uid() = from_user_id
);
CREATE POLICY "Owners can delete shares" ON public.shares FOR DELETE USING (
    auth.uid() = from_user_id
);

-- Share Likes RLS
ALTER TABLE public.share_likes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Likes are readable by all" ON public.share_likes FOR SELECT USING (true);
CREATE POLICY "Users can manage their own likes" ON public.share_likes FOR ALL USING (auth.uid() = user_id);

-- Share Saves RLS
ALTER TABLE public.share_saves ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Saves are private to user" ON public.share_saves FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can manage their own saves" ON public.share_saves FOR ALL USING (auth.uid() = user_id);

-- Inbox Items RLS
ALTER TABLE public.inbox_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see only their inbox items" ON public.inbox_items FOR ALL USING (auth.uid() = target_user_id);

-- Activity Log RLS
ALTER TABLE public.activity_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Activity log is private to user" ON public.activity_log FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert activity logs for themselves" ON public.activity_log FOR INSERT WITH CHECK (auth.uid() = user_id);


-- COMPETE REMOTE PROCEDURE CALLS (RPC FUNCTIONS FROM SCHEMA PARTE 1)

-- Import Share as Recipe
CREATE OR REPLACE FUNCTION public.import_share_as_recipe(input_share_id UUID)
RETURNS UUID AS $$
DECLARE
    src_share RECORD;
    new_recipe_id UUID;
    user_display_name TEXT;
BEGIN
    -- Validate user is logged in
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Usuario no autenticado';
    END IF;

    -- Fetch share snapshot
    SELECT * FROM public.shares WHERE id = input_share_id INTO src_share;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Publicacion compartida no encontrada';
    END IF;

    -- Get user display name
    SELECT display_name FROM public.profiles WHERE id = auth.uid() INTO user_display_name;

    -- Generate a unique private copy
    new_recipe_id := uuid_generate_v4();

    INSERT INTO public.recipes (
        id,
        user_id,
        owner_user_id,
        owner_display_name,
        name,
        method,
        coffee_grams,
        water_ml,
        ratio,
        temperature,
        clicks,
        notes,
        visibility,
        is_shared,
        original_author_user_id,
        original_author_name,
        original_entity_id,
        imported_from_share_id,
        copy_mode
    ) VALUES (
        new_recipe_id,
        auth.uid(),
        src_share.from_user_id,
        src_share.from_name,
        (src_share.payload_snapshot_json->>'name'),
        (src_share.payload_snapshot_json->>'method'),
        (src_share.payload_snapshot_json->>'coffeeGrams')::NUMERIC,
        (src_share.payload_snapshot_json->>'waterMl')::INTEGER,
        (src_share.payload_snapshot_json->>'ratio')::NUMERIC,
        (src_share.payload_snapshot_json->>'temperature')::INTEGER,
        (src_share.payload_snapshot_json->>'clicks'),
        (src_share.payload_snapshot_json->>'notes'),
        'private',
        FALSE,
        COALESCE(src_share.original_author_user_id, src_share.from_user_id),
        COALESCE(src_share.original_author_name, src_share.from_name),
        COALESCE(src_share.original_entity_id, src_share.entity_id),
        input_share_id,
        'original'
    );

    -- Log action
    INSERT INTO public.activity_log(user_id, action, entity_type, entity_id, share_id, note)
    VALUES (auth.uid(), 'import_recipe', 'recipe', new_recipe_id, input_share_id, 'Importó receta: ' || (src_share.payload_snapshot_json->>'name'));

    RETURN new_recipe_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Import Share as Technique
CREATE OR REPLACE FUNCTION public.import_share_as_technique(input_share_id UUID)
RETURNS UUID AS $$
DECLARE
    src_share RECORD;
    new_tech_id UUID;
    user_display_name TEXT;
    step_item JSONB;
BEGIN
    -- Validate user
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Usuario no autenticado';
    END IF;

    -- Fetch share snapshot
    SELECT * FROM public.shares WHERE id = input_share_id INTO src_share;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Publicacion compartida no encontrada';
    END IF;

    SELECT display_name FROM public.profiles WHERE id = auth.uid() INTO user_display_name;

    -- Generate technique
    new_tech_id := uuid_generate_v4();

    INSERT INTO public.techniques (
        id,
        user_id,
        owner_user_id,
        owner_display_name,
        name,
        method,
        coffee_grams,
        water_ml,
        ratio,
        temperature,
        grind_clicks,
        notes,
        visibility,
        is_shared,
        original_author_user_id,
        original_author_name,
        original_entity_id,
        imported_from_share_id,
        copy_mode
    ) VALUES (
        new_tech_id,
        auth.uid(),
        src_share.from_user_id,
        src_share.from_name,
        (src_share.payload_snapshot_json->>'name'),
        (src_share.payload_snapshot_json->>'method'),
        (src_share.payload_snapshot_json->>'coffeeGrams')::NUMERIC,
        (src_share.payload_snapshot_json->>'waterMl')::INTEGER,
        (src_share.payload_snapshot_json->>'ratio')::NUMERIC,
        (src_share.payload_snapshot_json->>'temperature')::INTEGER,
        (src_share.payload_snapshot_json->>'grindClicks'),
        (src_share.payload_snapshot_json->>'notes'),
        'private',
        FALSE,
        COALESCE(src_share.original_author_user_id, src_share.from_user_id),
        COALESCE(src_share.original_author_name, src_share.from_name),
        COALESCE(src_share.original_entity_id, src_share.entity_id),
        input_share_id,
        'original'
    );

    -- Insert technique steps from nested payload snapshot safely
    FOR step_item IN SELECT * FROM jsonb_array_elements(src_share.payload_snapshot_json->'steps')
    LOOP
        INSERT INTO public.technique_steps (
            technique_id,
            user_id,
            step_order,
            title,
            duration_sec,
            water_add_ml,
            target_water_ml,
            gesture,
            intensity,
            note
        ) VALUES (
            new_tech_id,
            auth.uid(),
            (step_item->>'step_order')::INTEGER,
            (step_item->>'title'),
            (step_item->>'duration_sec')::INTEGER,
            (step_item->>'water_add_ml')::INTEGER,
            (step_item->>'target_water_ml')::INTEGER,
            (step_item->>'gesture'),
            (step_item->>'intensity'),
            (step_item->>'note')
        );
    END LOOP;

    -- Log action
    INSERT INTO public.activity_log(user_id, action, entity_type, entity_id, share_id, note)
    VALUES (auth.uid(), 'import_technique', 'technique', new_tech_id, input_share_id, 'Importó técnica: ' || (src_share.payload_snapshot_json->>'name'));

    RETURN new_tech_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Fork Share as Recipe (fork allows the owner name to become current user and marks it editable copy)
CREATE OR REPLACE FUNCTION public.fork_share_as_recipe(input_share_id UUID)
RETURNS UUID AS $$
DECLARE
    src_share RECORD;
    new_recipe_id UUID;
    user_display_name TEXT;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Usuario no autenticado';
    END IF;

    SELECT * FROM public.shares WHERE id = input_share_id INTO src_share;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Publicacion no encontrada';
    END IF;

    SELECT display_name FROM public.profiles WHERE id = auth.uid() INTO user_display_name;

    new_recipe_id := uuid_generate_v4();

    INSERT INTO public.recipes (
        id,
        user_id,
        owner_user_id,
        owner_display_name,
        name,
        method,
        coffee_grams,
        water_ml,
        ratio,
        temperature,
        clicks,
        notes,
        visibility,
        is_shared,
        original_author_user_id,
        original_author_name,
        original_entity_id,
        imported_from_share_id,
        copy_mode
    ) VALUES (
        new_recipe_id,
        auth.uid(),
        auth.uid(), -- user themselves becomes current owner
        user_display_name,
        (src_share.payload_snapshot_json->>'name') || ' (Fork)',
        (src_share.payload_snapshot_json->>'method'),
        (src_share.payload_snapshot_json->>'coffeeGrams')::NUMERIC,
        (src_share.payload_snapshot_json->>'waterMl')::INTEGER,
        (src_share.payload_snapshot_json->>'ratio')::NUMERIC,
        (src_share.payload_snapshot_json->>'temperature')::INTEGER,
        (src_share.payload_snapshot_json->>'clicks'),
        (src_share.payload_snapshot_json->>'notes'),
        'private',
        FALSE,
        COALESCE(src_share.original_author_user_id, src_share.from_user_id),
        COALESCE(src_share.original_author_name, src_share.from_name),
        COALESCE(src_share.original_entity_id, src_share.entity_id),
        input_share_id,
        'fork'
    );

    INSERT INTO public.activity_log(user_id, action, entity_type, entity_id, share_id, note)
    VALUES (auth.uid(), 'fork_recipe', 'recipe', new_recipe_id, input_share_id, 'Hizo fork de receta: ' || (src_share.payload_snapshot_json->>'name'));

    RETURN new_recipe_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Fork Share as Technique
CREATE OR REPLACE FUNCTION public.fork_share_as_technique(input_share_id UUID)
RETURNS UUID AS $$
DECLARE
    src_share RECORD;
    new_tech_id UUID;
    user_display_name TEXT;
    step_item JSONB;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Usuario no autenticado';
    END IF;

    SELECT * FROM public.shares WHERE id = input_share_id INTO src_share;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Publicacion no encontrada';
    END IF;

    SELECT display_name FROM public.profiles WHERE id = auth.uid() INTO user_display_name;

    new_tech_id := uuid_generate_v4();

    INSERT INTO public.techniques (
        id,
        user_id,
        owner_user_id,
        owner_display_name,
        name,
        method,
        coffee_grams,
        water_ml,
        ratio,
        temperature,
        grind_clicks,
        notes,
        visibility,
        is_shared,
        original_author_user_id,
        original_author_name,
        original_entity_id,
        imported_from_share_id,
        copy_mode
    ) VALUES (
        new_tech_id,
        auth.uid(),
        auth.uid(),
        user_display_name,
        (src_share.payload_snapshot_json->>'name') || ' (Fork)',
        (src_share.payload_snapshot_json->>'method'),
        (src_share.payload_snapshot_json->>'coffeeGrams')::NUMERIC,
        (src_share.payload_snapshot_json->>'waterMl')::INTEGER,
        (src_share.payload_snapshot_json->>'ratio')::NUMERIC,
        (src_share.payload_snapshot_json->>'temperature')::INTEGER,
        (src_share.payload_snapshot_json->>'grindClicks'),
        (src_share.payload_snapshot_json->>'notes'),
        'private',
        FALSE,
        COALESCE(src_share.original_author_user_id, src_share.from_user_id),
        COALESCE(src_share.original_author_name, src_share.from_name),
        COALESCE(src_share.original_entity_id, src_share.entity_id),
        input_share_id,
        'fork'
    );

    FOR step_item IN SELECT * FROM jsonb_array_elements(src_share.payload_snapshot_json->'steps')
    LOOP
        INSERT INTO public.technique_steps (
            technique_id,
            user_id,
            step_order,
            title,
            duration_sec,
            water_add_ml,
            target_water_ml,
            gesture,
            intensity,
            note
        ) VALUES (
            new_tech_id,
            auth.uid(),
            (step_item->>'step_order')::INTEGER,
            (step_item->>'title'),
            (step_item->>'duration_sec')::INTEGER,
            (step_item->>'water_add_ml')::INTEGER,
            (step_item->>'target_water_ml')::INTEGER,
            (step_item->>'gesture'),
            (step_item->>'intensity'),
            (step_item->>'note')
        );
    END LOOP;

    INSERT INTO public.activity_log(user_id, action, entity_type, entity_id, share_id, note)
    VALUES (auth.uid(), 'fork_technique', 'technique', new_tech_id, input_share_id, 'Hizo fork de técnica: ' || (src_share.payload_snapshot_json->>'name'));

    RETURN new_tech_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- SEEDING SAMPLE PUBLIC DEMO RECORDS IN THE PUBLIC SCHEMA (OPTIONAL DEMO SEED)
-- Brew Methods Seed
INSERT INTO public.brew_methods (name, standard_ratio, description) VALUES 
('V60', 16.0, 'Extracción clásica cónica que resalta acidez y claridad.'),
('AeroPress', 13.0, 'Método híbrido de inmersión y presión altamente versátil.'),
('Prensa francesa', 15.0, 'Inmersión completa para un cuerpo pesado, rico en aceites.'),
('Chemex', 16.0, 'Filtros ultra-gruesos que producen una taza sumamente limpia.')
ON CONFLICT (name) DO NOTHING;
