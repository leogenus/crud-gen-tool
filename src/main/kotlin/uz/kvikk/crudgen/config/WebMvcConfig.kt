package uz.kvikk.crudgen.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.*


@Configuration
@EnableWebMvc
class WebMvcConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**").allowedOrigins("*").allowedMethods("*")
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.favorPathExtension(false)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/", "index.html")
    }
}
