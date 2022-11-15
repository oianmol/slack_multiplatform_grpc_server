package dev.baseio.slackserver.data.models

interface IDataMap {
  fun provideMap():HashMap<String,String>
}