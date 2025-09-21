-- добавляем уникальность названия подборки
ALTER TABLE compilations
    ADD CONSTRAINT uq_compilation_title UNIQUE (title);