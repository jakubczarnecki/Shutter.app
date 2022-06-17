import { Photo } from "components/photo";
import { PhotoMasonry } from "components/photo-masonry";

import React from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import styles from "./photographerGallery.module.scss";

export const PhotographerGallery = () => {
   const { t } = useTranslation();
   const { login } = useParams();
   // const { data, isError } = jakiestamquery()

   return (
      <section className={styles.photographer_gallery_page_wrapper}>
         <p className={styles.title}>{t("photographer_gallery_page.title")}</p>
         <PhotoMasonry login={login} />
      </section>
   );
};
