class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

//		"/"(view:"/index")
        "/"(controller: 'process')
        "500"(view:'/error')
	}
}
