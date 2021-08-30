package engine

import com.blocka.dns.BlockaDnsJNI
import model.BlokadaException
import service.EnvironmentService
import utils.Logger

object JniService {

    private val log = Logger("JniService")

    fun setup() {
        try {
            System.loadLibrary("blocka_dns")
        } catch (ex: Throwable) {
            throw BlokadaException("Could not load blocka_dns", ex)
        }

        try {
            System.loadLibrary("boringtun")
        } catch (ex: Throwable) {
            throw BlokadaException("Could not load boringtun", ex)
        }

        BlockaDnsJNI.engine_logger(if (EnvironmentService.isPublicBuild()) "error" else "debug")

//        val fromBlocka = BlockaDnsJNI.create_new_dns("127.0.0.1:8573", "1.1.1.1,1.0.0.1", "cloudflare-dns.com", "dns-query")
    }

}