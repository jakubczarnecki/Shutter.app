import React, { useEffect, useState } from "react";
import styles from "./photographersListPage.module.scss";
import { useTranslation } from "react-i18next";
import { MdKeyboardArrowRight } from "react-icons/md";
import { Card, TextInput } from "components/shared";
import { FaSearch } from "react-icons/fa";
import { PhotographerListRequest } from "redux/types/api";
import { useGetPhotographerListMutation } from "redux/service/photographerService";
import useDebounce from "hooks/useDebounce";
import { DisabledDropdown, ListElement } from "components/photographers-list";
import { AnimatePresence, motion } from "framer-motion";

export const PhotographersListPage = () => {
   const { t } = useTranslation();

   const [photographerSearchFilters, setPhotographerSearchFilters] =
      useState<PhotographerListRequest>({
         query: "",
         pageNo: 1,
         recordsPerPage: 25,
      });
   const [expandFilters, setExpandFilters] = useState(true);

   const debouncedFilters = useDebounce<PhotographerListRequest>(
      photographerSearchFilters,
      200
   );

   const [
      getPhotographers,
      {
         data: photographersListResponse = {
            allPages: 0,
            allRecords: 0,
            pageNo: 1,
            recordsPerPage: 25,
            list: [],
         },
         isSuccess: getPhotographersSuccess,
      },
   ] = useGetPhotographerListMutation();

   useEffect(() => {
      getPhotographers(debouncedFilters);
   }, [debouncedFilters]);

   const handleChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
      const name = e.target.name;
      setPhotographerSearchFilters({
         ...photographerSearchFilters,
         [name]: e.target.value,
      });
   };

   const filterCardContainerVariants = {
      animate: {
         height: expandFilters ? "auto" : 0,
         transition: {
            duration: 0.3,
            type: "tween",
            ease: "easeInOut",
         },
      },
   };

   const filterCardVariants = {
      initial: {
         opacity: 0,
      },
      animate: {
         opacity: 1,
         height: "auto",
         transition: {
            delay: 0.45,
            duration: 0.4,
            ease: "easeOut",
         },
      },
      exit: {
         opacity: 0,
         transition: {
            duration: 0.2,
         },
      },
   };

   return (
      <>
         <div className={`${styles.container}`}>
            <div className={styles.header}>
               <span className="category-title">{t("global.label.filters")}</span>
               <div className={styles.delimiter} />
               <motion.div
                  animate={{
                     rotate: expandFilters ? 90 : 0,
                  }}
               >
                  <button onClick={() => setExpandFilters(!expandFilters)}>
                     <MdKeyboardArrowRight
                        className={expandFilters ? "" : styles.expanded}
                     />
                  </button>
               </motion.div>
            </div>
            <motion.div
               className={`${styles.content}`}
               animate="animate"
               variants={filterCardContainerVariants}
            >
               <AnimatePresence>
                  {expandFilters && (
                     <>
                        <motion.div
                           key="search-filter"
                           variants={filterCardVariants}
                           initial="initial"
                           animate="animate"
                           exit="exit"
                        >
                           <Card className={styles.card}>
                              <p className="section-title">
                                 {t("photographer_list_page.search_photographer")}
                              </p>
                              <TextInput
                                 name="query"
                                 className={styles.input}
                                 icon={<FaSearch />}
                                 value={photographerSearchFilters.query}
                                 onChange={handleChange}
                              />
                           </Card>
                        </motion.div>
                     </>
                  )}
               </AnimatePresence>
            </motion.div>
         </div>

         <div className={styles.container}>
            <div className={styles.header}>
               <span className="category-title">
                  {t("photographer_list_page.photographers")}
               </span>
               <div className={styles.delimiter} />
               <div className={styles.functional}>
                  <p>
                     {getPhotographersSuccess &&
                        t("global.query.results", {
                           count: photographersListResponse.allRecords,
                        })}
                  </p>
                  <DisabledDropdown label="LIST" className={styles.dropdown} />
                  <DisabledDropdown label="A-Z" className={styles.dropdown} />
               </div>
            </div>
            <div className={`${styles.content} ${styles.list}`}>
               <AnimatePresence exitBeforeEnter>
                  {getPhotographersSuccess &&
                     photographersListResponse.list?.map((obj, i) => (
                        <ListElement
                           custom={i}
                           data={obj}
                           key={obj.login}
                           styles={styles}
                        />
                     ))}
               </AnimatePresence>
            </div>
         </div>
      </>
   );
};
