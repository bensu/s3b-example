(ns ^:figwheel-always example.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require [cljs.core.async :as async :refer (<! >! put! chan)]
              [om.core :as om :include-macros true]
              [om-tools.dom :as dom]
              [om-tools.core :refer-macros [defcomponent]]
              [s3-beam.client :as s3 :refer [s3-pipe s3-download-pipe]]))

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

(defonce app-state (atom {:text "File Uploader"
                          :link nil}))

(def signed-url "https://s3-beam-test.s3-eu-west-1.amazonaws.com/bddf0bb8-db04-4ed9-b032-5bb9b0e37de1?Signature=pAz%2BJFAUQTgN84Ru0ZbWIco3f6o%3D&AWSAccessKeyId=AKIAI7WJ6AVM37RFJMZQ&Expires=1432895218")

(defn file-icon [file owner {:keys [ch]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (let [file-name (.-name (:file file))]
          ;; FIX get the real file name to display
          (dom/div {:on-click (fn [_]
                                (go (>! ch (assoc (:response file)
                                             :file-name file-name))))} 
            file-name))))))

(defcomponent main [data owner]
  (init-state [_]
    (let [uploaded (chan)
          download (chan)]
      {:dropped-queue (chan)
       :upload-queue (s3-pipe uploaded) 
       :download download
       :download-queue (chan) 
       :uploaded uploaded
       :uploaded-files []
       :hoover? false}))
  (will-mount [_]
    (go-loop []
      (let [f (<! (om/get-state owner :uploaded))]
        (js/alert (str "The file " (.-name (:file f)) " was succesfully uploaded"))
        (om/update-state! owner :uploaded-files #(conj % f)))
      (recur))
    (go-loop []
      (let [res (<! (om/get-state owner :download))]
        (.log js/console res))
      (recur))
    (go-loop []
      (let [f (<! (om/get-state owner :download-queue))
            ch (chan)]
        (go (let [url (<! ch)]
              (om/update! data :link {:url url
                                      :file-name (:file-name f)})))
        (println f)
        (s3/sign-download f ch))
      (recur)))
  (render-state [_ {:keys [upload-queue download-queue]}]
    (dom/div
      (dom/h1 (:text data))
      (dom/a {:href (:url (:link data))} (:file-name (:link data)))
      (apply dom/ul nil
        (map #(om/build file-icon % {:opts {:ch download-queue}})
          (om/get-state owner :uploaded-files)))
      (om/build drop-area {} {:opts {:ch upload-queue}}))))

(om/root main app-state {:target (. js/document (getElementById "app"))})

(defn on-js-reload [] nil) 
