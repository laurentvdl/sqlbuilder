package sqlbuilder.kotlin.select.join

import sqlbuilder.ResultSet
import sqlbuilder.rowhandler.JoiningRowHandler

inline fun <reified T : Any> JoiningRowHandler<T>.mapPrimaryBean(set: ResultSet, prefix: String): T = this.mapPrimaryBean(set, T::class.java, prefix)