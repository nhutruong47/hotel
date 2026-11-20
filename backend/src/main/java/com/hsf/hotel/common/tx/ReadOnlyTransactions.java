package com.hsf.hotel.common.tx;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Runs a unit of read work inside a read-only transaction.
 *
 * <p>Production runs with {@code spring.jpa.open-in-view=false}, so any lazy
 * association touched after a service transaction has committed throws
 * {@code LazyInitializationException}. Controllers must therefore build their
 * response DTOs <em>inside</em> a transaction — typically by reloading the
 * aggregate by id and mapping it here — instead of serializing JPA entities.
 */
@Component
public class ReadOnlyTransactions {

    @Transactional(readOnly = true)
    public <T> T run(Supplier<T> work) {
        return work.get();
    }
}
