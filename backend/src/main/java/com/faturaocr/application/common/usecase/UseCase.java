package com.faturaocr.application.common.usecase;

/**
 * Generic interface for all use cases.
 * Each use case represents a single application operation.
 * 
 * @param <I> Input type (Command/Query)
 * @param <O> Output type (Response)
 */
public interface UseCase<I, O> {

    O execute(I input);
}
