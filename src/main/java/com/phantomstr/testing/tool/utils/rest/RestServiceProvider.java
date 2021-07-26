package com.phantomstr.testing.tool.utils.rest;

import retrofit2.Retrofit;

/**
 * Поставщик сервисов для отправки сообщений.
 */
public interface RestServiceProvider {

    /**
     * Получение сервиса по интерфейсу.
     * <p>
     * Подробно почитать про сервисы можно в <a href="https://square.github.io/retrofit/" target="_top">документации</a>
     *
     * @param serviceClass Интерфейс сервиса для отправки сообщений
     * @param <T>          класс интерфейса.
     * @return T
     */
    default <T> T getService(Class<T> serviceClass) {
        return getRetrofit().create(serviceClass);
    }

    /**
     * Получение экземпляра класса Retrofit.
     *
     * @return Retrofit экземпляр.
     */
    Retrofit getRetrofit();

}
