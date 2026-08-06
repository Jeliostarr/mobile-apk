package com.example.data.api

import com.example.data.model.Movie
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object ResponseEnvelopeExtractor {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val movieAdapter = moshi.adapter(Movie::class.java)

    fun extractMovieList(jsonString: String): List<Movie> {
        try {
            val genericAdapter = moshi.adapter(Any::class.java)
            val jsonObject = genericAdapter.fromJson(jsonString) ?: return emptyList()

            // Case 1: Bare List [...]
            if (jsonObject is List<*>) {
                val listType = Types.newParameterizedType(List::class.java, Movie::class.java)
                return moshi.adapter<List<Movie>>(listType).fromJson(jsonString) ?: emptyList()
            }

            // Case 2: Map {...}
            if (jsonObject is Map<*, *>) {
                // Check data level
                val data = jsonObject["data"]
                if (data is List<*>) {
                    val dataJson = moshi.adapter(Any::class.java).toJson(data)
                    val listType = Types.newParameterizedType(List::class.java, Movie::class.java)
                    return moshi.adapter<List<Movie>>(listType).fromJson(dataJson) ?: emptyList()
                } else if (data is Map<*, *>) {
                    val movies = data["movies"]
                    if (movies is List<*>) {
                        val moviesJson = moshi.adapter(Any::class.java).toJson(movies)
                        val listType = Types.newParameterizedType(List::class.java, Movie::class.java)
                        return moshi.adapter<List<Movie>>(listType).fromJson(moviesJson) ?: emptyList()
                    }
                    val innerData = data["data"]
                    if (innerData is Map<*, *>) {
                        val innerMovies = innerData["movies"]
                        if (innerMovies is List<*>) {
                            val innerMoviesJson = moshi.adapter(Any::class.java).toJson(innerMovies)
                            val listType = Types.newParameterizedType(List::class.java, Movie::class.java)
                            return moshi.adapter<List<Movie>>(listType).fromJson(innerMoviesJson) ?: emptyList()
                        }
                    }
                }

                // Check movies directly at root
                val moviesDirect = jsonObject["movies"]
                if (moviesDirect is List<*>) {
                    val moviesJson = moshi.adapter(Any::class.java).toJson(moviesDirect)
                    val listType = Types.newParameterizedType(List::class.java, Movie::class.java)
                    return moshi.adapter<List<Movie>>(listType).fromJson(moviesJson) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    fun extractSingleMovie(jsonString: String): Movie? {
        try {
            val genericAdapter = moshi.adapter(Any::class.java)
            val jsonObject = genericAdapter.fromJson(jsonString) ?: return null

            if (jsonObject is Map<*, *>) {
                // Check if _id is directly in root
                if (jsonObject.containsKey("_id") || jsonObject.containsKey("title")) {
                    return movieAdapter.fromJson(jsonString)
                }

                // Check data level
                val data = jsonObject["data"]
                if (data is Map<*, *>) {
                    if (data.containsKey("_id") || data.containsKey("title")) {
                        val dataJson = moshi.adapter(Any::class.java).toJson(data)
                        return movieAdapter.fromJson(dataJson)
                    }
                    val nestedData = data["data"]
                    if (nestedData is Map<*, *>) {
                        val nestedJson = moshi.adapter(Any::class.java).toJson(nestedData)
                        return movieAdapter.fromJson(nestedJson)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
