import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './FileInput.module.scss';

interface Props {
   className?: string;
   onFileChange?: React.ChangeEventHandler<HTMLInputElement>;
   file: File;
}

export const FileInput: React.FC<Props> = ({className, onFileChange, file}) => {

    const { t } = useTranslation();

    const [labelData, setLabelData] = useState({ labelText: t("global.label.choose_file")});

    return (
        <div className={`${styles.text_input_wrapper} ${className ? className : ""}`}>
            <label >
                {labelData.labelText}
                <input type="file" onChange={(e) => {
                    file = e.target.files[0];
                    setLabelData({ labelText: file.name });
                    onFileChange(e);
                }}/>
            </label>
        </div>
    )
}