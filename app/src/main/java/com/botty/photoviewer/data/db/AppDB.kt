package com.botty.photoviewer.data.db

import com.botty.photoviewer.di.repos.ConnectionsRepo
import com.botty.photoviewer.di.repos.DBFilesRepo
import com.botty.photoviewer.di.repos.DBFoldersRepo
import com.botty.photoviewer.di.repos.GalleriesRepo

interface AppDB : ConnectionsRepo, GalleriesRepo, DBFoldersRepo, DBFilesRepo