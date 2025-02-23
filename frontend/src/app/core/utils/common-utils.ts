import { parseISO, format } from 'date-fns';

export function dateParserSend(key: string, value: any) {
  if (value instanceof Date) {
    return format(value, "yyyy-MM-dd'T'HH:mm:ss");
  }

  if (typeof value === 'string') {
    if (value.trim() === '') {
      return null;
    }

    const regexISO = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*))(?:Z|([\+|-])([\d|:]*))?$/;
    const match = regexISO.exec(value);
    if (match) {
      return format(parseISO(value), "yyyy-MM-dd'T'HH:mm:ss");
    }
  }

  return value;
}

export function dateParser(key: string, value: any) {
  if (typeof value === 'string') {
    let a = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*))(?:Z|([\+|-])([\d|:]*))?$/.exec(value);
    if (a) {
      return new Date(value);
    }
    a = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})?$/.exec(value);
    if (a) {
      return new Date(value);
    }
    a = /^\/Date\((.*)\)[\/|\\]$/.exec(value);
    if (a) {
      let b = a[1].split(/[-+,.]/);
      return new Date(b[0] ? +b[0] : 0 - +b[1]);
    }
    a = /^(\d{1,4})-(\d{1,2})-(\d{1,2})$/.exec(value);
    if (a) {
      const sa = value.split('-');
      return new Date(parseInt(sa[0], 10), parseInt(sa[1], 10) - 1, parseInt(sa[2], 10));
    }
  }
  return value;
}

export function biggest(...values: number[]): number {
  return [...values].sort((a, b) => b - a)[0];
}

export function round(value: number, scale: number = 2) {
  scale = Math.abs(scale);
  if (scale === 0) return Math.trunc(value);
  return +(Math.round(Number.parseFloat(`${value}e+${scale}`)) + `e-${scale}`);
}

export function isPropertyAssignable(obj: any, prop: string): boolean {
  return isDefined(Object.getOwnPropertyDescriptor(obj, prop));
}

export function isDefined(val: any) {
  return val !== undefined;
}

export function isDefinedAndNotNull(val: any) {
  return val !== undefined && val !== null;
}

export function isNumber(val: any) {
  return !isNaN(parseFloat(val)) && isFinite(val);
}

export function isNullOrEmpty(obj: any): boolean {
  return !!(!obj || Object.keys(obj).length === 0);
}

export function isPromise(value: any) {
  return value instanceof Promise;
}

export function isFunction(value: any) {
  return value instanceof Function;
}

export function isString(value: any): boolean {
  return typeof value === 'string' || value instanceof String;
}

export function isArray(...arrays: any[]) {
  return arrays?.every(arr => arr instanceof Array);
}

export function isObject(...objs: any[]) {
  return objs?.every(obj => (typeof obj === 'object' || obj instanceof Object) && isDefinedAndNotNull(obj));
}

export function isArrayOrObject(val: any) {
  return isArray(val) || isObject(val);
}

export function isBlob(value: any) {
  return value instanceof Blob;
}

export function deepCopy<T>(model: T): T {
  return JSON.parse(JSON.stringify(model));
}

export function hasProperties(...objs: Object[]): boolean {
  return objs?.every(obj => !!Object.keys(obj).length);
}

export function hasSameProperties(a: Object, b: Object): boolean {
  var aKeys = Object.keys(a).sort();
  var bKeys = Object.keys(b).sort();
  return JSON.stringify(aKeys) === JSON.stringify(bKeys);
}

export function hasSpecificProperties(properties: string[], ...objs: Object[]): boolean {
  return objs?.every(obj => Object.keys(obj).every(k => properties.every(prop => k === prop)));
}

export function countProperties(obj: Object): number {
  return Object.keys(obj).length;
}

export function equalsObjects(a: any, b: any): boolean {
  return (
    a === b ||
    !(a && b) ||
    !(a?.length && b?.length) ||
    equalsByStringify(a, b) ||
    equalsArray(a, b) ||
    (isObject(a, b) &&
      hasProperties(a, b) &&
      countProperties(a) === countProperties(b) &&
      Object.keys(a).every(p => equalsObjects(a[p], b[p])))
  );
}

export function equalsByStringify(a: any, b: any): boolean {
  return a === b || JSON.stringify(a) === JSON.stringify(b);
}

export function equalsArray<T>(a: Array<T>, b: Array<T>): boolean {
  return (
    !(a?.length && b?.length) ||
    (a?.length === b?.length && isArray(a, b) && a.every(el => b.includes(el)))
  );
}

export function copy(o: any) {
  return Object.assign({}, o);
}
