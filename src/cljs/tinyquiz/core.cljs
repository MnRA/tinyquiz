(ns tinyquiz.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]
   [ajax.core :refer [GET]]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/questions"
     ["" :questions]
     ["/:question-id" :question]]
    ["/final-score" :final-score]
    ]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(defn goto-question
  ([question-id] (goto-question question-id false))
  ([question-id final-question]
   (if final-question
     (accountant/navigate! (path-for :final-score))
     (accountant/navigate! (path-for :question {:question-id question-id})))))

;; State
(def results (atom {}))

(def questionaire (atom nil))

(defn get-questionaire []
  (GET "/questionaire" :handler #(reset! questionaire %)
       :response-format :json))


;; Page components



(defn home-page []
  (get-questionaire)
  (fn []
    [:span.main
     [:h1 "Welcome to tinyquiz"]
     [:h2 "What's your name?"]
     [:input {:type "text"
              :value (:name @results)
              :on-change #(swap! results assoc :name (-> % .-target .-value))
              :on-key-down #(case (.-which %)
                              13 (goto-question 1))}]
     [:button {:on-click #(goto-question 1)} "Start Quiz"]]))


(defn final-score-page []
  (fn []
    [:span.main
     [:h1 (str "Congratulations " (:name @results) "! You completed the quiz!")]
     [:h2 "Your final score was: "]
     [:h1 (apply + (vals (:results @results)))]]))

(defn question-page []
  (fn []
    (let [routing-data (session/get :route)
          question-id (get-in routing-data [:route-params :question-id])
          question-data (clojure.walk/keywordize-keys
                         (get @questionaire question-id))
          name (:name question-data)]
      [:span.main {:on-key-down #(if (= 13 (.-which %))
                                   (goto-question (inc (int question-id))
                                                  (:final-question question-data)))}
       [:h1 (str "This is question " question-id)]
       [:h2 (:question question-data)]
       (map (fn [[option-name option-val]]
              [:div {:key option-name}
               [:label {:for name } option-name]
               [:input {:type :radio

                        :name name
                        :on-change #(swap! results assoc-in [:results name] option-val)}]
               [:br]] )
            (:options question-data))
       [:button {:on-click #(if (= question-id "1")
                              (accountant/navigate! (path-for :index))
                              (goto-question (dec (int question-id))))}
        "Previous"]

       [:button {:on-click #(goto-question
                             (inc (int question-id))
                             (:final-question question-data))}
        "Next"]
       ])))



;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :question #'question-page
    :final-score #'final-score-page))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:p [:a {:href (path-for :index)} "Home"]]]
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
