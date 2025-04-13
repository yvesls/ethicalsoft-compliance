import { Subject } from "rxjs";

export interface ModalRef {
  close: (result?: any) => void;
  result: Subject<any>;
}
