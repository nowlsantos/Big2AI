package com.nowlsantos.big2ai.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

// This is not a module, just the annotation for the scope