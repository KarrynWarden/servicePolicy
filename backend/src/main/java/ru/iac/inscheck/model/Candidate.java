package ru.iac.inscheck.model;

/**
 * Результат работы алгоритма поиска: найденная запись IPerson, по которой
 * определяется СК (id) и СК* (idmain — записи с одинаковым IDMain считаются
 * одним СК*, п.4.4).
 */
public record Candidate(long id, Long idmain) {
}
