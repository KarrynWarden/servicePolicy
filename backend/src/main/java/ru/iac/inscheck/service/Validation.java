package ru.iac.inscheck.service;

import ru.iac.inscheck.model.ErrCode;
import ru.iac.inscheck.model.SearchParams;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Результат проверки входных данных (раздел 2 постановки):
 * накопленные ошибки и нормализованные параметры поиска.
 */
public class Validation {

    private final Set<ErrCode> errors = new LinkedHashSet<>();
    private final SearchParams params = new SearchParams();

    public void add(ErrCode code) {
        errors.add(code);
    }

    public List<ErrCode> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean hasFatal() {
        return errors.stream().anyMatch(ErrCode::isFatal);
    }

    public SearchParams getParams() {
        return params;
    }
}
