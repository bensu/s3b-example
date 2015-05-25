(ns ^:figwheel-always example.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require [cljs.core.async :as async :refer (<! >! put! chan)]
              [om.core :as om :include-macros true]
              [om-tools.dom :as dom]
              [om-tools.core :refer-macros [defcomponent]]
              [s3-beam.client :refer [s3-pipe]]))

(enable-console-print!)

(defn handle-drop
  "Extracts the files out of the event e and puts them in the channel"
  [ch e]
  (.preventDefault e)
  (.stopPropagation e)
  (let [fs (or (.. e -target -files) 
               (.. e -target -value)
               (.. e -dataTransfer -files))]
    (put! ch (aget fs 0))))

(defcomponent file-input [data owner {:keys [ch]}]
  (render [_]
    (dom/input {:type "file"
                :on-change (partial handle-drop ch)})))

(defcomponent drop-area [data owner {:keys [ch]}]
  (render-state [_ {:keys [hoover?]}]
    (dom/div {:class (str "drop-area " (when hoover? "dragging"))
              :on-drag-over (fn [e]
                              (.preventDefault e)
                              (set! (.. e -dataTransfer -dropEffect) "copy")
                              (when-not hoover?
                                (om/set-state! owner :hoover? true)))
              :on-drag-leave (fn [_]
                               (when hoover?
                                 (om/set-state! owner :hoover? false)))
              :on-drop (fn [e]
                         (om/set-state! owner :hoover? false)
                         (handle-drop ch e))}
      (om/build file-input {} {:opts {:ch ch}}))))

(defonce app-state (atom {:text "File Uploader"}))

(defcomponent main [data owner]
  (init-state [_]
    (let [uploaded (chan)]
      {:dropped-queue (chan)
       :upload-queue (s3-pipe uploaded) 
       :uploaded uploaded
       :hoover? false}))
  (will-mount [_]
    (go-loop []
      (let [fs (<! (om/get-state owner :uploaded))]
        (js/alert (str "The file " (.-name (:file fs)) " was succesfully uploaded")))
      (recur)))
  (render-state [_ {:keys [upload-queue]}]
    (dom/div
      (dom/h1 (:text data))
      (om/build drop-area {} {:opts {:ch upload-queue}}))))

(om/root main app-state {:target (. js/document (getElementById "app"))})

(defn on-js-reload [] nil) 
