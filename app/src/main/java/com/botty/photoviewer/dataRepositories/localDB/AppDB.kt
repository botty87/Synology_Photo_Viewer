package com.botty.photoviewer.dataRepositories.localDB

interface AppDB : ConnectionsRepo,
    GalleriesRepo,
    DBFoldersRepo,
    DBFilesRepo