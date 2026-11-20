package com.bookstore.pos.servicio;

import com.bookstore.pos.modelo.Libro;
import com.bookstore.pos.repositorio.LibroRepository;
import java.util.List;

/**
 * Servicio encargado de gestionar la lógica de negocio del inventario de libros (RF01).
 * Aplica programación defensiva, validación de precondiciones y bajo acoplamiento
 * mediante la inyección de dependencias de {@link LibroRepository}.
 *
 * @author Pedro Román Domínguez Rodríguez
 * @version 1.0
 */
public class InventarioService {

    // Subtema 1: Uso de constantes con nombres significativos para evitar "números mágicos"
    private static final String PATRON_ISBN_VALIDO = "\\d{10}|\\d{13}";
    private static final double PRECIO_MINIMO_EXCLUSIVO = 0.0;
    private static final int STOCK_MINIMO_PERMITIDO = 0;
    private static final int CANTIDAD_MINIMA_OPERACION = 1;

    private final LibroRepository repositorio;

    /**
     * Construye el servicio inyectando la implementación del repositorio de libros.
     *
     * @param repositorio Implementación de acceso a datos de libros.
     * @throws IllegalArgumentException si el repositorio proporcionado es nulo.
     */
    public InventarioService(LibroRepository repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("El repositorio de libros no puede ser nulo.");
        }
        this.repositorio = repositorio;
    }

    /**
     * Registra un nuevo libro en el catálogo tras validar sus reglas de integridad.
     *
     * @param libro Entidad {@link Libro} con los datos del título a registrar.
     * @return El objeto {@link Libro} persistido en el repositorio.
     * @throws IllegalArgumentException si el libro es nulo o incumple validaciones de formato/rango.
     * @throws IllegalStateException si ya existe un libro registrado con el mismo ISBN.
     */
    public Libro registrarLibro(Libro libro) {
        // Subtema 2 y 3: Cláusulas de guarda secuenciales para validar precondiciones
        validarDatosObligatoriosLibro(libro);

        if (repositorio.existePorIsbn(libro.getIsbn())) {
            throw new IllegalStateException("Ya existe un libro registrado con el ISBN: " + libro.getIsbn());
        }

        repositorio.guardar(libro);
        return libro;
    }

    /**
     * Consulta un libro registrado mediante su código ISBN.
     *
     * @param isbn Código identificador ISBN de 10 o 13 dígitos.
     * @return El {@link Libro} correspondiente al ISBN solicitado.
     * @throws IllegalArgumentException si el ISBN es inválido o no existe en el catálogo.
     */
    public Libro consultarPorIsbn(String isbn) {
        validarFormatoIsbn(isbn);

        return repositorio.buscarPorIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún libro con el ISBN: " + isbn));
    }

    /**
     * Descuenta unidades del stock disponible tras confirmarse una venta.
     *
     * @param isbn Código ISBN del libro vendido.
     * @param cantidadVendida Número de ejemplares a descontar (debe ser >= 1).
     * @throws IllegalArgumentException si la cantidad es menor a 1 o el libro no existe.
     * @throws IllegalStateException si el stock actual es menor a la cantidad solicitada.
     */
    public void descontarStock(String isbn, int cantidadVendida) {
        if (cantidadVendida < CANTIDAD_MINIMA_OPERACION) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero.");
        }

        Libro libro = consultarPorIsbn(isbn);

        if (libro.getStock() < cantidadVendida) {
            throw new IllegalStateException("Stock insuficiente para el libro: " + libro.getTitulo());
        }

        int nuevoStock = libro.getStock() - cantidadVendida;
        libro.setStock(nuevoStock);
        repositorio.actualizar(libro);
    }

    /**
     * Incrementa las existencias físicas de un libro en el inventario.
     *
     * @param isbn Código ISBN del libro a reabastecer.
     * @param cantidadNueva Número de ejemplares que ingresan al almacén (debe ser >= 1).
     * @throws IllegalArgumentException si la cantidad es menor a 1 o el libro no existe.
     */
    public void agregarStock(String isbn, int cantidadNueva) {
        if (cantidadNueva < CANTIDAD_MINIMA_OPERACION) {
            throw new IllegalArgumentException("La cantidad a agregar debe ser mayor a cero.");
        }

        Libro libro = consultarPorIsbn(isbn);

        int nuevoStock = libro.getStock() + cantidadNueva;
        libro.setStock(nuevoStock);
        repositorio.actualizar(libro);
    }

    /**
     * Elimina un libro del catálogo siempre que no cuente con existencias activas.
     *
     * @param isbn Código ISBN del libro a dar de baja.
     * @return {@code true} si el libro fue eliminado correctamente.
     * @throws IllegalStateException si el libro aún posee stock mayor a cero.
     */
    public boolean darDeBajaLibro(String isbn) {
        Libro libro = consultarPorIsbn(isbn);

        if (libro.getStock() > STOCK_MINIMO_PERMITIDO) {
            throw new IllegalStateException(
                    "No se puede dar de baja un libro con stock disponible (" + libro.getStock() + " unidades)."
            );
        }

        return repositorio.eliminar(isbn);
    }

    /**
     * Recupera la lista completa de libros almacenados en el inventario.
     *
     * @return Lista de todos los objetos {@link Libro} registrados.
     */
    public List<Libro> obtenerCatalogoCompleto() {
        return repositorio.listarTodos();
    }

    public List<Libro> buscarPorTitulo(String textoBusqueda) {
        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            throw new IllegalArgumentException("El criterio de búsqueda por título no puede estar vacío.");
        }
        String criterio = textoBusqueda.trim().toLowerCase();
        return repositorio.listarTodos().stream()
                .filter(libro -> libro.getTitulo() != null && libro.getTitulo().toLowerCase().contains(criterio))
                .toList();
    }
    
    // Subtema 4: Descomposición funcional en métodos auxiliares privados cohesivos
    private void validarDatosObligatoriosLibro(Libro libro) {
        if (libro == null) {
            throw new IllegalArgumentException("El objeto libro no puede ser nulo.");
        }

        validarFormatoIsbn(libro.getIsbn());

        if (libro.getTitulo() == null || libro.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título del libro es obligatorio.");
        }

        if (libro.getPrecioUnitario() <= PRECIO_MINIMO_EXCLUSIVO) {
            throw new IllegalArgumentException("El precio unitario debe ser mayor a cero.");
        }

        if (libro.getStock() < STOCK_MINIMO_PERMITIDO) {
            throw new IllegalArgumentException("El stock inicial no puede ser negativo.");
        }
    }

    private void validarFormatoIsbn(String isbn) {
        if (isbn == null || !isbn.matches(PATRON_ISBN_VALIDO)) {
            throw new IllegalArgumentException("El ISBN debe contener exactamente 10 o 13 dígitos numéricos.");
        }
    }
}