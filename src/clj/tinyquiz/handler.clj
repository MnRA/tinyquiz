(ns tinyquiz.handler
  (:require
   [reitit.ring :as reitit-ring]
   [tinyquiz.middleware :refer [middleware]]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]))

(def mount-target
  [:div#app
   [:h2 "Welcome to tinyquiz"]
   [:p "please wait while Figwheel is waking up ..."]
   [:p "(Check the js console for hints if nothing exciting happens.)"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js "/js/app.js")]))

(defn questionaire-handler [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body {1 {:question "What is your favorite versioning tool?"
                          :name :question1
                          :options {"SVN" 0
                                    "GIT" 10
                                    "CVS" -5
                                    "Mercurial" 5
                                    "HUHH?" 0}}
                      2 {:question "What is your favorite cat?"
                          :name :question2
                          :options {"Ceiling cat" 10
                                    "Invisible bike cat" 5
                                    "Octocat" 8
                                    "Grumpy cat" 12
                                    "Monorail cat" 2}}
                      3 {:question "What is your favorite jewel?"
                          :name :question3
                          :options {"Diamond" 5
                                    "Ruby" 10
                                    "Kryptonite" 10
                                    "Emerald" -5}
                         :final-question true}
                      }})


(defn index-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/questionaire" {:get {:handler questionaire-handler}}]
     ["/questions"
      ["/:question-id" {:get {:handler index-handler
                              :parameters {:path {:question-id int?}}}}]]
     ])
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware middleware}))
