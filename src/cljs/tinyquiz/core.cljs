(ns tinyquiz.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/questions"
     ["" :questions]
     ["/:question-id" :question]]
    ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components
(def results (atom {}))
(def questions (atom {1 {:question "What is your favorite versioning tool?"
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
                                    "Emerald" -5}}
                      }
                     ))

(defn goto-question [question-id] (accountant/navigate! (path-for :question {:question-id question-id})))


(defn home-page []
  (fn []
    [:span.main
     [:h1 "Welcome to tinyquiz"]
     [:h2 "What's your name?"
      [:input {:type "text"
               :value (:name @results)
               :on-change #(swap! results assoc :name (-> % .-target .-value))
               :on-key-down #(case (.-which %)
                               13 (goto-question 1))}]]]))
      [:li [:a {:href (path-for :items)} "Items of tinyquiz"]]

(defn question-page []
  (fn []
    (let [routing-data (session/get :route)
          question-id (get-in routing-data [:route-params :question-id])
          question-data (get @questions (int question-id))
          name (:name question-data)]
      (println @questions)
      [:span.main {:on-key-down #(if (= 13 (.-which %))
                                   (goto-question (inc (int question-id))))}
       [:p (str "question values: " question-data)]
       [:h1 (str "This is question " question-id)]
       [:h2 (:question question-data)
        (map (fn [[option-name option-val]]
               [:div {:key option-name}
                [:label {:for name} option-name]
                [:input {:type :radio

                         :name name
                         :on-change #(swap! results assoc-in [:results name] option-val)}]] )
             (:options question-data))]
       ])))


(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of tinyquiz"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))



(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of tinyquiz")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))


(defn about-page []
  (fn [] [:span.main
          [:h1 "About tinyquiz"]]))


;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page
    :question #'question-page))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:p [:a {:href (path-for :index)} "Home"] " | "
         [:a {:href (path-for :about)} "About tinyquiz"]
         [:p "current status: " @results]]]
       [page]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
