(ns boodle.expenses.views
  (:require [boodle.categories.views :as cv]
            [boodle.common :as common]
            [boodle.i18n :refer [translate]]
            [boodle.modal :as modal]
            [boodle.pikaday :as pikaday]
            [boodle.validation :as v]
            [re-frame.core :as rf]))

(defn render-row
  [row]
  [:tr {:key (random-uuid)}
   [:td (:date row)]
   [:td (:category row)]
   [:td (:item row)]
   [:td (str (common/format-number (:amount row)) (translate :it :currency))]
   [:td.has-text-centered
    (when (:from-savings row) [:i.fa.fa-check])]
   [:td
    [:nav.level
     [:div.level-item.has-text-centered
      [:div.field.is-grouped.is-grouped-centered
       [:p.control
        [:button.button
         {:on-click #(rf/dispatch [:edit-expense (:id row)])}
         [:i.fa.fa-pencil]]]
       [:p.control
        [:button.button
         {:on-click #(rf/dispatch [:remove-expense (:id row)])}
         [:i.fa.fa-remove]]]]]]]])

(defn expenses-table
  []
  (fn []
    (let [rows (rf/subscribe [:expenses-rows])]
      [:table.table.is-striped.is-fullwidth
       [:thead
        [:tr
         [:th (translate :it :expenses/table.date)]
         [:th (translate :it :expenses/table.category)]
         [:th (translate :it :expenses/table.item)]
         [:th (translate :it :expenses/table.amount)]
         [:th.has-text-centered
          (translate :it :expenses/table.from-savings)]
         [:th.has-text-centered
          (translate :it :expenses/table.actions)]]]
       [:tbody
        (doall (map render-row @rows))]])))

(defn search-fields
  []
  (fn []
    (let [categories (conj @(rf/subscribe [:categories]) {:id "" :name ""})
          params (rf/subscribe [:expenses-params])]
      [:nav.level
       [:div.level-item.has-text-centered
        [:div.field.is-horizontal
         [:div.field-label.is-normal
          [:label.label (translate :it :expenses/label.from)]]
         [:div.field-body
          [:div.field
           [pikaday/date-selector
            {:date-atom (rf/subscribe [:expenses-from])
             :pikaday-attrs
             {:onSelect #(rf/dispatch [:expenses-change-from %])
              :format "DD/MM/YYYY"}}]]]]]
       [:div.level-item.has-text-centered
        [:div.field.is-horizontal
         [:div.field-label.is-normal
          [:label.label (translate :it :expenses/label.to)]]
         [:div.field-body
          [:div.field
           [pikaday/date-selector
            {:date-atom (rf/subscribe [:expenses-to])
             :pikaday-attrs {:onSelect #(rf/dispatch [:expenses-change-to %])
                             :format "DD/MM/YYYY"}}]]]]]
       [:div.level-item.has-text-centered
        [:div.field.is-horizontal
         [:div.field-label.is-normal
          [:label.label (translate :it :expenses/label.category)]]
         [:div.field-body
          [:div.field
           [:div.select
            [:select
             {:value (v/or-empty-string (:categories @params))
              :on-change #(rf/dispatch [:expenses-change-categories
                                        (-> % .-target .-value)])}
             (map common/render-option categories)]]]]]]])))

(defn expenses-buttons
  []
  (fn []
    [:div.field.is-grouped.is-grouped-centered
     [:p.control
      [:button.button.is-primary
       {:on-click #(rf/dispatch [:get-expenses-by-date])}
       (translate :it :expenses/button.search)]]
     [:p.control
      [:button.button.is-danger
       {:on-click #(rf/dispatch [:reset-search])}
       (translate :it :expenses/button.reset)]]
     [:p.control
      [:button.button.is-primary
       {:on-click #(rf/dispatch [:create-expense])}
       (translate :it :expenses/button.add)]]]))

(defn current-month
  []
  (fn []
    (let [current-date (js/Date.)
          months (:months pikaday/i18n)
          month (nth months (.getMonth current-date))
          year (.getFullYear current-date)]
      [:h5.title.is-5.has-text-centered.is-uppercase
       (str month " " year)])))

(defn categories-cards
  []
  (fn []
    (let [categories @(rf/subscribe [:categories-monthly-expenses])]
      [:div.columns.is-multiline
       (doall
        (for [c categories]
          [:div.column.is-2
           {:key (:id c)}
           [:div.card
            [:header.card-header
             [:p.card-header-title.is-centered
              (:name c)]]
            [:div.card-content
             [:div.content.has-text-centered
              [:p (str (common/format-number (:total c))
                       (translate :it :currency)
                       " / "
                       (common/format-number (:monthly-budget c))
                       (translate :it :currency))]]]]]))])))

(defn home-panel
  []
  (fn []
    [:div
     [common/header]

     [:div.container
      [common/page-title (translate :it :expenses/page.title)]
      [v/validation-msg-box]

      [current-month]
      [categories-cards]

      [:hr]

      [search-fields]
      [expenses-buttons]
      [modal/modal]

      [:div {:style {:padding-top ".1em"}}
       [expenses-table]]]]))
