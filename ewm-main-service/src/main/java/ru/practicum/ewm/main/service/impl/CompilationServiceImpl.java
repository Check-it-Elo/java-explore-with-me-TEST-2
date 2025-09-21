package ru.practicum.ewm.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.NewCompilationDto;
import ru.practicum.ewm.main.dto.UpdateCompilationRequest;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CompilationMapper;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.repository.CompilationRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.service.CompilationService;
import ru.practicum.ewm.main.util.PageUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;

    @Transactional
    @Override
    public CompilationDto create(NewCompilationDto dto) {
        Set<Event> events = resolveEvents(dto.getEvents());
        Compilation comp = compilationMapper.fromNew(dto, events);
        comp = compilationRepository.save(comp);
        return toDtoWithShorts(comp);
    }

    @Transactional
    @Override
    public void delete(long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Transactional
    @Override
    public CompilationDto update(long compId, UpdateCompilationRequest dto) {
        Compilation comp = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        Set<Event> events = dto.getEvents() == null ? comp.getEvents() : resolveEvents(dto.getEvents());
        compilationMapper.update(dto, comp, events);
        if (dto.getTitle() != null) comp.setTitle(dto.getTitle());
        if (dto.getPinned() != null) comp.setPinned(dto.getPinned());
        comp = compilationRepository.save(comp);
        return toDtoWithShorts(comp);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        Page<Compilation> page = (pinned == null)
                ? compilationRepository.findAll(PageUtils.by(from, size))
                : compilationRepository.findAllByPinned(pinned, PageUtils.by(from, size));
        return page.stream().map(this::toDtoWithShorts).collect(Collectors.toList());
    }

    @Override
    public CompilationDto getById(long compId) {
        Compilation comp = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return toDtoWithShorts(comp);
    }

    // ===== helpers =====

    private Set<Event> resolveEvents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        List<Event> events = eventRepository.findAllById(ids);
        if (events.size() != ids.size()) {
            throw new NotFoundException("Some events were not found");
        }
        return new HashSet<>(events);
    }

    private CompilationDto toDtoWithShorts(Compilation comp) {
        List<EventShortDto> shorts = comp.getEvents() == null ? List.of()
                : comp.getEvents().stream()
                .map(e -> eventMapper.toShortDto(e, 0L, 0L))
                .collect(Collectors.toList());
        return compilationMapper.toDto(comp, shorts);
    }
}