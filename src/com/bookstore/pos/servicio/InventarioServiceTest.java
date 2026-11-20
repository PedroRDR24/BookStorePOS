package com.bookstore.pos.servicio;

import com.bookstore.pos.modelo.Libro;
import com.bookstore.pos.repositorio.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InventarioServiceTest {

    private InventarioService inventarioService;
    private InMemoryLibroRepository fakeRepository;

    // Implementación en memoria para aislar la lógica de negocio de JDBC/SQL
    static class InMemoryLibroRepository implements LibroRepository {
        private final Map<String, Libro> baseDatos = new HashMap<>();

        @Override
        public void guardar(Libro libro) { baseDatos.put(libro.getIsbn(), libro); }

        @Override
        public Optional<Libro> buscarPorIsbn(String isbn) { return Optional.ofNullable(baseDatos.get(isbn)); }

        @Override
        public List<Libro> listarTodos() { return new ArrayList<>(baseDatos.values()); }

        @Override
        public void actualizar(Libro libro) { baseDatos.put(libro.getIsbn(), libro); }

        @Override
        public boolean eliminar(String isbn) { return baseDatos.remove(isbn) != null; }

        @Override
        public boolean existePorIsbn(String isbn) { return baseDatos.containsKey(isbn); }
    }

    @BeforeEach
    void setUp() {
        fakeRepository = new InMemoryLibroRepository();
        inventarioService = new InventarioService(fakeRepository);
    }

    @Test
    @DisplayName("CP01 - Registro exitoso de un libro válido")
    void testRegistrarLibroValido() {
        Libro libro = new Libro("9786070728792", "Cien años de soledad", "Gabriel García Márquez",
                "Diana", "Novela", 349.00, 15);

        Libro resultado = inventarioService.registrarLibro(libro);

        assertNotNull(resultado);
        assertEquals("9786070728792", resultado.getIsbn());
        assertEquals(15, inventarioService.consultarPorIsbn("9786070728792").getStock());
    }

    @Test
    @DisplayName("CP02 - Rechazar registro cuando el ISBN es duplicado")
    void testRegistrarLibroIsbnDuplicado() {
        Libro libro1 = new Libro("9780132350884", "Clean Code", "Robert C. Martin",
                "Prentice Hall", "Tecnología", 850.00, 5);
        Libro libroDuplicado = new Libro("9780132350884", "Clean Code 2da Ed", "Robert C. Martin",
                "Prentice Hall", "Tecnología", 900.00, 3);

        inventarioService.registrarLibro(libro1);

        IllegalStateException excepcion = assertThrows(IllegalStateException.class, () -> {
            inventarioService.registrarLibro(libroDuplicado);
        });
        assertTrue(excepcion.getMessage().contains("Ya existe un libro registrado"));
    }

    @Test
    @DisplayName("CP03 - Rechazar registro con formato de ISBN inválido")
    void testRegistrarLibroIsbnInvalido() {
        Libro libroInvalido = new Libro("123-ABC", "El Principito", "Antoine de Saint-Exupéry",
                "Salamandra", "Infantil", 199.00, 10);

        assertThrows(IllegalArgumentException.class, () -> {
            inventarioService.registrarLibro(libroInvalido);
        });
    }

    @Test
    @DisplayName("CP04 - Rechazar registro con precio unitario cero o negativo")
    void testRegistrarLibroPrecioInvalido() {
        Libro libroPrecioCero = new Libro("9788498387087", "Harry Potter y la piedra filosofal",
                "J.K. Rowling", "Salamandra", "Fantasía", 0.0, 8);

        assertThrows(IllegalArgumentException.class, () -> {
            inventarioService.registrarLibro(libroPrecioCero);
        });
    }

    @Test
    @DisplayName("CP05 - Descuento correcto de stock tras venta")
    void testDescontarStockExitoso() {
        Libro libro = new Libro("9786073152624", "1984", "George Orwell",
                "Debolsillo", "Ciencia Ficción", 229.00, 10);
        inventarioService.registrarLibro(libro);

        inventarioService.descontarStock("9786073152624", 4);

        assertEquals(6, inventarioService.consultarPorIsbn("9786073152624").getStock());
    }

    @Test
    @DisplayName("CP06 - Impedir descuento de stock mayor a las existencias disponibles")
    void testDescontarStockInsuficiente() {
        Libro libro = new Libro("9786070743931", "Pedro Páramo", "Juan Rulfo",
                "RM", "Clásicos", 210.00, 2);
        inventarioService.registrarLibro(libro);

        assertThrows(IllegalStateException.class, () -> {
            inventarioService.descontarStock("9786070743931", 5);
        });
        // Verificar que el stock intacto sigue siendo 2
        assertEquals(2, inventarioService.consultarPorIsbn("9786070743931").getStock());
    }

    @Test
    @DisplayName("CP07 - Reabastecimiento de stock existente")
    void testAgregarStock() {
        Libro libro = new Libro("9788420412146", "Don Quijote de la Mancha", "Miguel de Cervantes",
                "Alfaguara", "Clásicos", 420.00, 3);
        inventarioService.registrarLibro(libro);

        inventarioService.agregarStock("9788420412146", 7);

        assertEquals(10, inventarioService.consultarPorIsbn("9788420412146").getStock());
    }

    @Test
    @DisplayName("CP08 - Baja de libro exitosa cuando el stock es cero")
    void testDarDeBajaLibroSinStock() {
        Libro libro = new Libro("9786071133441", "El túnel", "Ernesto Sabato",
                "Booket", "Novela", 180.00, 0);
        inventarioService.registrarLibro(libro);

        boolean eliminado = inventarioService.darDeBajaLibro("9786071133441");

        assertTrue(eliminado);
        assertThrows(IllegalArgumentException.class, () -> {
            inventarioService.consultarPorIsbn("9786071133441");
        });
    }

    @Test
    @DisplayName("CP09 - Impedir baja de libro si aún existen unidades en stock")
    void testDarDeBajaLibroConStockActivo() {
        Libro libro = new Libro("9788466338141", "La sombra del viento", "Carlos Ruiz Zafón",
                "Booket", "Misterio", 299.00, 4);
        inventarioService.registrarLibro(libro);

        assertThrows(IllegalStateException.class, () -> {
            inventarioService.darDeBajaLibro("9788466338141");
        });
    }
}