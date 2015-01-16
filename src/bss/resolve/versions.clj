(ns bss.resolve.versions
  (:require [clojure.string :as str]))

(defn expand-version [ver-str]
  (let [[base metadata]     (str/split ver-str #"\+" 2)
        [maj min patch pre] (str/split base #"\.|-" 4)]
    ;; ignore metadata
    (mapv #(%2 %1)
          [maj min patch pre #_metadata]
          [#(if % (Long/parseLong %) 0)
           #(if % (Long/parseLong %) 0)
           #(if % (Long/parseLong %) 0)
           #(or % "")
           #_identity])))

(defn print-version [expanded]
  (let [[pre metadata] (drop 3 expanded)
        [maj min patch] (map #(or % 0) (take 3 expanded))]
    (let [base (if (empty? pre)
                 (format "%s.%s.%s" maj min patch)
                 (format "%s.%s.%s-%s" maj min patch pre))]
      (if-not metadata
        base
        (str base "+" metadata)))))

(def normalize (comp print-version expand-version))

(defn bump-expanded [[maj min patch pre :as expanded]]
  (cond (not (empty? pre))
        [maj min patch ""]

        (> maj 0)
        [(inc maj) 0 0 ""]

        (> min 0)
        [0 (inc min) 0 ""]

        :else
        [0 0 (inc patch) ""]))

(def bump (comp print-version bump-expanded expand-version))

(defn- core- [version] (vec (take 3 version)))
(defn- snap- [version] (first (drop 3 version)))

(defn- compare- [a b]
  (let [raw (compare (core- a) (core- b))]
    (if (not= 0 raw)
      raw
      ;; metadata does not matter, only snapshot.
      ;; no snapshot > a snapshot
      ;; sort snapshots alphabetically
      (let [a (snap- a)
            b (snap- b)]
        (cond (and (empty? a) (not (empty? b))) 1
              (and (empty? b) (not (empty? a))) -1
              :else (compare a b))))))

(defn- sort-versions [versions] (sort compare- versions))

(defn- filter-min-max
  "Filter to min <= x < max"
  [min max versions]
  (let [pred (fn [v] (and (>= (compare v min) 0)
                         (neg? (compare v max))))]
    (filter pred versions)))

(defn match [versions & [version]]
  (if version
    (if (re-find #"^\^" version)
      ;; semantic upgrade, aka caret range
      (let [min (expand-version (.substring version 1))
            max (bump-expanded min)
            top (last (sort-versions (filter-min-max min max versions)))]
        (if top (print-version top)))
      ;; exact
      (let [v (first (filter #{(expand-version version)} versions))]
        (if v (print-version v))))
    ;; use latest
    (if-let [max (last (sort-versions versions))]
      (print-version max))))
