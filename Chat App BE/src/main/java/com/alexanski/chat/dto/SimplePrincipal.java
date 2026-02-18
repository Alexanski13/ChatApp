package com.alexanski.chat.dto;

import java.security.Principal;
import java.util.Objects;

public record SimplePrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimplePrincipal that = (SimplePrincipal) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}