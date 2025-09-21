package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.NewCompilationDto;
import ru.practicum.ewm.main.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    // admin
    CompilationDto create(NewCompilationDto dto);

    void delete(long compId);

    CompilationDto update(long compId, UpdateCompilationRequest dto);

    // public
    List<CompilationDto> getAll(Boolean pinned, int from, int size);

    CompilationDto getById(long compId);
}