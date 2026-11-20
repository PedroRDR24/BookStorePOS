package com.bookstore.pos.repositorio;

import com.bookstore.pos.modelo.Libro;
import java.util.List;
import java.util.Optional;

public interface LibroRepository {
    void guardar(Libro libro);
    Optional<Libro> buscarPorIsbn(String isbn);
    List<Libro> listarTodos();
    void actualizar(Libro libro);
    boolean eliminar(String isbn);
    boolean existePorIsbn(String isbn);
}