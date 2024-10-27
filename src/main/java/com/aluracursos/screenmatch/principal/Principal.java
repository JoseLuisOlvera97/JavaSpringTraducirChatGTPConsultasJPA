package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;
import org.aspectj.weaver.ISourceContext;
import org.aspectj.weaver.ast.Var;
import org.springframework.boot.origin.SystemEnvironmentOrigin;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "http://www.omdbapi.com/?";
    //private final String API_KEY = ${OMDB_KEY};
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();

    private SerieRepository repositorio;
    private List<Serie> series;
    private Optional<Serie>serieBuscada;

    public Principal(SerieRepository repository) {
        this.repositorio = repository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar series 
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar series por titulo
                    5 - Top 5 mejores series
                    6 - Buscar serie por categoria
                    7 - Filtrar series por temporada y evaluaciones
                    8 - Buscar episodios por titulo
                    9 - Top 5 episodios por serie
                                  
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarTop5Series();
                    break;
                case 6:
                    buscarSeriePorCategoria();
                    break;
                case 7:
                    filtrarSeriesPorTemporadaYEvaluaciones();
                    break;
                case 8:
                    buscarEpisodioPorTitulo();
                    break;
                case 9:
                    buscarTop5Episodios();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }

    }
    //Metodos
    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + System.getenv("OMDB_KEY") + nombreSerie.replace(" ", "+"));
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }
    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("Ecsribe el nombre de la serie que quieres ver los episodios");
        var nombreSerie = teclado.nextLine();
        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nombreSerie.toLowerCase()))
                .findFirst();

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();

            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + System.getenv("OMDB_KEY") + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream()
                            .flatMap(d -> d.episodios().stream()
                                    .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }
    }
    private void buscarSerieWeb() {
        //GUardar busquedas en BDD
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        repositorio.save(serie);
        //datosSeries.add(datos);
        System.out.println(datos);
    }

    private void mostrarSeriesBuscadas() {
        //Mostrar busquedas desde la BDD
        series = repositorio.findAll();


                  //Mostrar por listas las busqeudas echas en el momento
//        List<serie> series = new ArrayList<>();
//        series = datosSeries.stream()
//                .map(d -> new serie(d))
//                .collect(Collectors.toList());
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo(){
        System.out.println("Ecsribe el nombre de la serie que desea buscar");
        var nombreSerie = teclado.nextLine();
         serieBuscada = repositorio.findByTituloContainsIgnoreCase(nombreSerie);

        if (serieBuscada.isPresent()){
            System.out.println("La serie buscada es: " + serieBuscada.get());
        }else {
            System.out.println("Serie no encontrada");
        }
    }

    private void buscarTop5Series(){
        List<Serie> topSeries = repositorio.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s ->
                System.out.println("Serie; " + s.getTitulo() + "Evaluacion: " + s.getEvaluacion()));
    }

    private void buscarSeriePorCategoria(){
        System.out.println("Ecriba el genero / categoria de la serie que desea buscar");
        var genero = teclado.nextLine();
        var categoria = Categoria.fromsEspanol(genero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Las series de las categoria" + genero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void filtrarSeriesPorTemporadaYEvaluaciones(){
        System.out.println("Filtrar series con cuantas temporadas");
        var totaltemporadas = teclado.nextInt();
        teclado.nextLine();
        System.out.println("Con evaluacion de cual valor");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaYEvaluacion(totaltemporadas, evaluacion);
        System.out.println("***Series filtradas***");
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + " - evaluacion: " + s.getEvaluacion()));
    }

    private void buscarEpisodioPorTitulo(){
        System.out.println("Escribe el nombre del episodio que seseas buscar");
        var nombreEpisodio = teclado.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodioPorNOmbre(nombreEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Serie: %s Temporada %s Episodio %s Evaluacion %s\n",
                        e.getSerie(), e.getTemporada(), e.getNumeroEpisodio(),e.getEvaluacion()));
    }

    private void buscarTop5Episodios(){
        buscarSeriePorTitulo();
        if (serieBuscada.isPresent()){
            Serie serie =serieBuscada.get();
            List<Episodio> topEpisodios = repositorio.top5Episodios(serie);
            topEpisodios.forEach(e ->
                    System.out.printf("Serie: %s - Temporada %s - Episodio %s - Evaluacion %s\n",
                            e.getSerie(), e.getTemporada(), e.getNumeroEpisodio(),e.getEvaluacion()));
        }
    }



}

