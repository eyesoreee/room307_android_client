package com.example.room307.di

import com.example.room307.files.data.repository.FileRepositoryImp
import com.example.room307.files.domain.repository.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepo(
        fileRepoImpl: FileRepositoryImp
    ): FileRepository
}