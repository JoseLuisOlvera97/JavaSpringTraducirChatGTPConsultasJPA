package com.aluracursos.screenmatch.repository;

import com.aluracursos.screenmatch.model.Categoria;
import com.aluracursos.screenmatch.model.Episodio;
import com.aluracursos.screenmatch.model.Serie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SerieRepository extends JpaRepository<Serie, Long> {
Optional<Serie> findByTituloContainsIgnoreCase(String nombreSerie);
List<Serie> findTop5ByOrderByEvaluacionDesc();
List<Serie> findByGenero(Categoria categoria);

//Filtrar series por evaluacion y temporadas
@Query("SELECT s FROM Serie s WHERE s.totalTemporadas <= :totalTemporadas AND s.evaluacion >= :evaluacion")
    List<Serie> seriesPorTemporadaYEvaluacion(int totalTemporadas, Double evaluacion);

//BUscar episodios por nombre
@Query("SELECT e FROM Serie s JOIN s.episodios e WHERE e.titulo ILIKE %:nombreEpisodio%")
    List<Episodio> episodioPorNOmbre(String nombreEpisodio);

//top5episodio
@Query("SELECT e FROM Serie s JOIN s.episodios e WHERE s = :serie ORDER BY e.evaluacion DESC LIMIT 5 ")
List<Episodio> top5Episodios(Serie serie);

}
