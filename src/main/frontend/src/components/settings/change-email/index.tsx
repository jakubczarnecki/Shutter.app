import Button from "components/shared/Button";
import Card from "components/shared/Card";
import React from "react";
import { useSendChangeOwnEmailLinkMutation } from "redux/service/api";

const ChangeEmail = () => {
   const [mutation, mutationState] = useSendChangeOwnEmailLinkMutation();

   return (
      <Card id="change-email">
         <p className="category-title">Adres email</p>
         <p>
            W przypadku chęci zmiany adresu email zostanie wysłany odpowiedni link na twój
            aktualny adres email.
         </p>
         <Button
            onClick={() => {
               mutation({});
            }}
         >
            Wyślij link
         </Button>

         {mutationState.isError && <p>Nie udało się wysłać wiadomości</p>}
         {mutationState.isSuccess && <p>Wiadomość została wysłana</p>}
      </Card>
   );
};

export default ChangeEmail;