package rearth.oritech.api.energy.containers;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingEnergyStorage extends EnergyApi.EnergyStorage {
    
    protected final Supplier<EnergyApi.EnergyStorage> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingEnergyStorage(Supplier<EnergyApi.EnergyStorage> backingStorage, @Nullable BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
    }
    
    public DelegatingEnergyStorage(EnergyApi.EnergyStorage backingStorage, @Nullable BooleanSupplier validPredicate) {
        this(() -> backingStorage, validPredicate);
    }
    
    @Override
    public long getCapacity() {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                return storage.getCapacity();
            }
        }
        return 0;
    }

    @Override
    public void update() {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                storage.update();
            }
        }
    }

    @Override
    public long insert(long amount, boolean simulate) {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                return storage.insert(amount, simulate);
            }
        }
        return 0;
    }

    @Override
    public long extract(long amount, boolean simulate) {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                return storage.extract(amount, simulate);
            }
        }
        return 0;
    }
    
    @Override
    public boolean supportsInsertion() {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                return storage.supportsInsertion();
            }
        }
        return false;
    }

    @Override
    public boolean supportsExtraction() {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                return storage.supportsExtraction();
            }
        }
        return false;
    }

    @Override
    public void setAmount(long amount) {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                storage.setAmount(amount);
            }
        }
    }

    @Override
    public long getAmount() {
        if (validPredicate.getAsBoolean()) {
            var storage = backingStorage.get();
            if (storage != null) {
                return storage.getAmount();
            }
        }
        return 0;
    }
}
