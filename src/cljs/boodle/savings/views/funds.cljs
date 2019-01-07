(ns boodle.savings.views.funds
  (:require [boodle.common :as common]
            [boodle.i18n :refer [translate]]
            [re-frame.core :as rf]))

(defn total
  []
  (fn []
    (let [rows @(rf/subscribe [:funds])
          total (:total rows)]
      [:nav.level
       [:div.level-item.has-text-centered
        [:h5.title.is-size-5 (translate :it :funds/label.total-funds)
         (str (common/format-number total) (translate :it :currency))]]])))

(defn render-row
  [row]
  (when-let [amount (:amount row)]
    (let [amount-str (common/format-neg-or-pos amount)
          color (if (pos? amount) common/green common/red)]
      [:tr {:key (random-uuid)}
       [:td (:date row)]
       [:td (:name row)]
       [:td
        {:style {:color color}}
        (str amount-str (translate :it :currency))]])))

(defn table
  []
  (fn []
    (let [rows @(rf/subscribe [:funds])
          funds (:funds rows)]
      [:div {:style {:padding-bottom "1em"}}
       [:table.table.is-striped.is-fullwidth
        [:thead
         [:tr
          [:th (translate :it :funds/table.date)]
          [:th (translate :it :funds/table.name)]
          [:th (translate :it :funds/table.amount)]]]
        [:tbody
         (doall (map render-row funds))]]])))